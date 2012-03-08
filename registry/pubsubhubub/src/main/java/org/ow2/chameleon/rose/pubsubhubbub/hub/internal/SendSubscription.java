package org.ow2.chameleon.rose.pubsubhubbub.hub.internal;

import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_UPDATE_CONTENT;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED;

import java.io.IOException;
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
import org.ow2.chameleon.json.JSONService;

/**
 * Sending notifications to subscribers.
 * 
 * @author Bartek
 * 
 */
public class SendSubscription {

	private enum SendOptions {
		sendAfterSubscribe, sendAfterPublisherUpdate, sendAfterTopicDelete
	}

	private EndpointDescription edp;
	private String callBackUrl;
	private HttpPost postMethod;
	private String updateOption;
	private String rssURL;
	private HttpClient client;
	private RegistrationsImpl registrations;
	private JSONService json;
	private LogService logger;

	/**
	 * Main constructor
	 * 
	 * @param pClient
	 *            Client {@link HttpClient}to send notifications
	 * @param pRegistrations
	 *            Endpoints,subscribers,publishers management
	 * @param pJson
	 *            {@link JSONService}
	 * @param pLogger
	 *            {@link LogService}
	 */
	public SendSubscription(HttpClient pClient, RegistrationsImpl pRegistrations,
			LogService pLogger, JSONService pJson) {
		this.client = pClient;
		this.registrations = pRegistrations;
		this.logger = pLogger;
		this.json = pJson;
	}

	/**
	 * Prepare a thread to send an endpointDescriptions after subscriber
	 * appears.
	 * 
	 * @param pCallBackUrl
	 *            full url address of subscriber
	 */
	public void subscriberAdded(final String pCallBackUrl) {
		this.callBackUrl = pCallBackUrl;
		this.updateOption = HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED;
		(new SendingUpdateThread(SendOptions.sendAfterSubscribe)).start();
	}

	/**
	 * Prepare a thread to send an endpointDescriptions after publisher update.
	 * 
	 * @param pEdp
	 *            notified @EndpointDescription
	 * @param pUpdateOption
	 *            update option: endpoint.add or endpoint.remove
	 */
	public void endpoindUpdated(final EndpointDescription pEdp,
			final String pUpdateOption) {
		this.edp = pEdp;
		this.updateOption = pUpdateOption;
		(new SendingUpdateThread(SendOptions.sendAfterPublisherUpdate)).start();
	}

	/**
	 * Prepare a thread to send a remove endpointDescription to all subscribers
	 * when topic is deleted.
	 * 
	 * @param pRssUrl
	 *            url address to RSS
	 * @param pUpdateOption
	 *            update option: endpoint.add or endpoint.remove
	 */
	public void topicDelete(final String pRssUrl) {
		this.rssURL = pRssUrl;
		this.updateOption = HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED;
		(new SendingUpdateThread(SendOptions.sendAfterTopicDelete)).start();
	}

	private class SendingUpdateThread extends Thread {

		private SendOptions option;

		public SendingUpdateThread(SendOptions option) {
			this.option = option;
		}

		/**
		 * Send an endpointDescriptions after subscriber appears.
		 */

		private void sendAfterSubscribe() {
			try {
				for (EndpointDescription endp : registrations
						.getEndpointsForCallBackUrl(callBackUrl)) {
					sendUpdate(endp, callBackUrl);
				}
			} finally {
				registrations.releaseReadLock();
			}

		}

		/**
		 * Send an endpointDescriptions after publisher update.
		 */
		private void sendAfterPublisherUpdate() {
			try {
				for (String callBackURL : registrations
						.getSubscribersByEndpoint(edp)) {
					sendUpdate(edp, callBackURL);
					if (updateOption.equals("endpoint.remove")) {
						registrations
								.removeInterestedEndpoint(callBackURL, edp);
					}
				}
			} finally {
				registrations.releaseReadLock();
			}

		}

		/**
		 * Send a remove endpointDescription to subscribers when topic is
		 * deleted.
		 */
		private void sendAfterTopicDelete() {
			try {
				for (Map.Entry<String, Set<EndpointDescription>> entry : registrations
						.getSubscriberAndEndpointsByPublisherRssUrl(rssURL)
						.entrySet()) {
					for (EndpointDescription endpoint : entry.getValue()) {
						sendUpdate(endpoint, entry.getKey());
					}

				}
			} finally {
				registrations.releaseReadLock();
			}
			registrations.clearTopic(rssURL);
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
			nvps.add(new BasicNameValuePair(
					HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION, updateOption));
			nvps.add(new BasicNameValuePair(HTTP_POST_UPDATE_CONTENT, json
					.toJSON(endpoint.getProperties())));
			try {
				postMethod
						.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

				final HttpResponse response = client.execute(postMethod);
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					logger.log(LogService.LOG_ERROR,
							"Error in sending an update to subscriber: "
									+ callBackUrl + ", got response "
									+ response.getStatusLine().getStatusCode());
					response.getEntity().getContent().close();
				}
				// read an empty entity and close a connection
				response.getEntity().getContent().close();
			} catch (Exception e) {
				logger.log(LogService.LOG_ERROR,
						"Error in sending an update to subscriber", e);
			}
		}

		@Override
		public final void run() {
			switch (option) {
			case sendAfterPublisherUpdate:
				this.sendAfterPublisherUpdate();
				break;
			case sendAfterSubscribe:
				this.sendAfterSubscribe();
				break;
			case sendAfterTopicDelete:
				this.sendAfterTopicDelete();
				break;

			}

		}
	}
}
