package org.ow2.chameleon.rose.pubsubhubbub.hub.internal;

import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_UPDATE_CONTENT;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_UPDATE_TOPIC_DELETE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Sending a notification to subscribers.
 * 
 * @author Bartek
 * 
 */
public class SendSubscription extends Thread {

	private EndpointDescription edp;
	private String callBackUrl;
	private HttpPost postMethod;
	private HubImpl server;
	private String updateOption;
	private String rssURL;
	private HttpClient client;

	/**
	 * Prepare a thread to send an endpointDescriptions after subscriber
	 * appears.
	 * 
	 * @param pClient
	 *            client to send data
	 * @param pCallBackUrl
	 *            full url address of subscriber
	 * @param pUpdateOption
	 *            update option: endpoint.add or endpoint.remove
	 * @param pServer
	 *            bridge to Hub
	 */
	public SendSubscription(final HttpClient pClient,
			final String pCallBackUrl, final String pUpdateOption,
			final HubImpl pServer) {
		this.callBackUrl = pCallBackUrl;
		this.client = pClient;
		this.server = pServer;
		this.updateOption = pUpdateOption;
	}

	/**
	 * Prepare a thread to send an endpointDescriptions after publisher update.
	 * 
	 * @param pClient
	 *            to send data
	 * @param pEdp
	 *            notified @EndpointDescription
	 * @param pUpdateOption
	 *            update option: endpoint.add or endpoint.remove
	 * @param pServer
	 *            bridge to Hub
	 */
	public SendSubscription(final HttpClient pClient,
			final EndpointDescription pEdp, final String pUpdateOption,
			final HubImpl pServer) {
		this.edp = pEdp;
		this.client = pClient;
		this.server = pServer;
		this.updateOption = pUpdateOption;
	}

	/**
	 * Prepare a thread to send a remove endpointDescription to all subscribers
	 * when topic is deleted.
	 * 
	 * @param pClient
	 *            to send data
	 * @param pRssUrl
	 *            url address to RSS
	 * @param pTopicDelete
	 *            topic.remove
	 * @param pServer
	 *            bridge to Hub
	 * @param pUpdateOption
	 *            update option: endpoint.add or endpoint.remove
	 */
	public SendSubscription(final HttpClient pClient, final String pRssUrl,
			final String pUpdateOption, final HubImpl pServer,
			final String pTopicDelete) {
		if (pTopicDelete.equals(HUB_UPDATE_TOPIC_DELETE)) {
			this.rssURL = pRssUrl;
			this.client = pClient;
			this.server = pServer;
			this.updateOption = pUpdateOption;
		}
	}

	/**
	 * Send an endpointDescriptions after subscriber appears.
	 */

	private void sendAfterSubscribe() {
		for (EndpointDescription endp : server.registrations()
				.getEndpointsForCallBackUrl(this.callBackUrl)) {
			sendUpdate(endp, this.callBackUrl);
		}
	}

	/**
	 * Send an endpointDescriptions after publisher update.
	 */
	private void sendAfterPublisherUpdate() {
		for (String callBackURL : server.registrations()
				.getSubscribersByEndpoint(this.edp)) {
			sendUpdate(edp, callBackURL);
			if (updateOption.equals("endpoint.remove")) {
				server.registrations().removeInterestedEndpoint(callBackURL,
						edp);
			}
		}

	}

	/**
	 * Send a remove endpointDescription to subscribers when topic is deleted.
	 */
	private void sendAfterTopicDelete() {
		final Map<String, Set<EndpointDescription>> subscriberEndpoins = server
				.registrations().getEndpointsAndSubscriberByPublisher(rssURL);

		for (Map.Entry<String, Set<EndpointDescription>> entry : subscriberEndpoins
				.entrySet()) {
			for (EndpointDescription endpoint : entry.getValue()) {
				sendUpdate(endpoint, entry.getKey());
			}

		}
		server.registrations().clearTopic(this.rssURL);
	}

	/**
	 * General method to send notification to subscribers.
	 * 
	 * @param endpoint
	 *            endpointDescription which is notified
	 * @param pCallBackUrl
	 *            url address where to send a notification
	 * @throws IOException
	 */
	private void sendUpdate(final EndpointDescription endpoint,
			final String pCallBackUrl) {

		postMethod = new HttpPost(pCallBackUrl);
		postMethod.setHeader("Content-Type",
				"application/x-www-form-urlencoded");

		final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair(HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION,
				updateOption));
		nvps.add(new BasicNameValuePair(HTTP_POST_UPDATE_CONTENT, server.json()
				.toJSON(endpoint.getProperties())));
		try {
			postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

			final HttpResponse response = client.execute(postMethod);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				server.logger().log(
						LogService.LOG_ERROR,
						"Error in sending an update to subscriber: "
								+ callBackUrl + ", got response "
								+ response.getStatusLine().getStatusCode());
				response.getEntity().getContent().close();
			}
			// read an empty entity and close a connection
			response.getEntity().getContent().close();
		} catch (Exception e) {
			server.logger().log(LogService.LOG_ERROR,
					"Error in sending an update to subscriber", e);
		}
	}

	@Override
	public final void run() {
		if (callBackUrl != null) {
			sendAfterSubscribe();
		} else if (edp != null) {
			sendAfterPublisherUpdate();
		} else if (rssURL != null) {
			sendAfterTopicDelete();
		}
	}

}
