package org.ow2.chameleon.rose.pubsubhubbub.hub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Sending a notification to subscribers
 * 
 * @author Bartek
 * 
 */
public class SendSubscription extends Thread {

	private EndpointDescription edp;
	private String callBackUrl;
	private HttpPost postMethod;
	private Hub server;
	private String updateOption;
	private String rssURL;
	private HttpClient client;

	/**
	 * Run a thread to send an endpointDescriptions after subscriber appears
	 * 
	 * @param client
	 * @param callBackUrl
	 * @param updateOption
	 * @param server
	 * @param registrations
	 */
	public SendSubscription(HttpClient client, String callBackUrl,
			String updateOption, Hub server) {
		this.callBackUrl = callBackUrl;
		this.client = client;
		this.server = server;
		this.updateOption = updateOption;
		this.start();
	}

	/**
	 * Run a thread to send an endpointDescriptions after publisher update
	 * 
	 * @param client
	 * @param edp
	 * @param server
	 * @param registrations
	 */
	public SendSubscription(HttpClient client, EndpointDescription edp,
			String updateOption, Hub server) {
		this.edp = edp;
		this.client = client;
		this.server = server;
		this.updateOption = updateOption;
		this.start();
	}

	/**
	 * Run a thread to send a remove endpointDescription to all subscribers when
	 * topic is deleted
	 * 
	 * @param client
	 * @param rssUrl
	 * @param topicDelete
	 * @param endpointRemove
	 * @param server2
	 */
	public SendSubscription(HttpClient client, String rssUrl,
			String updateOption, Hub server, String topicDelete) {
		if (topicDelete.equals("topic.delete")) {
			this.rssURL = rssUrl;
			this.client = client;
			this.server = server;
			this.updateOption = updateOption;
			this.start();
		}
	}

	/**
	 * Send an endpointDescriptions after subscriber appears
	 */

	private void sendAfterSubscribe() {
		for (EndpointDescription edp : server.registrations()
				.getEndpointsForCallBackUrl(this.callBackUrl)) {
			try {
				sendUpdate(edp, this.callBackUrl);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Send an endpointDescriptions after publisher update
	 */
	private void sendAfterPublisherUpdate() {
		for (String callBackUrl : server.registrations()
				.getSubscribersByEndpoint(this.edp)) {
			try {
				sendUpdate(edp, callBackUrl);
				if (updateOption.equals("endpoint.remove")) {
					server.registrations().removeInterestedEndpoint(
							callBackUrl, edp);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Send a remove endpointDescription to subscribers when topic is deleted
	 */
	private void sendAfterTopicDelete() {
		Map<String, Set<EndpointDescription>> subscriberEndpoins = server
				.registrations().getEndpointsAndSubscriberByPublisher(rssURL);

		for (String subscriber : subscriberEndpoins.keySet()) {
			for (EndpointDescription endpoint : subscriberEndpoins
					.get(subscriber)) {
				try {
					sendUpdate(endpoint, subscriber);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		server.registrations().clearTopic(this.rssURL);
	}

	/**
	 * General method to send notification to subscribers
	 * 
	 * @param edp
	 *            endpointDescription which is notified
	 * @param callBackUrl
	 *            url address where to send a notification
	 * @throws IOException
	 */
	private void sendUpdate(EndpointDescription edp, String callBackUrl)
			throws IOException {

		postMethod = new HttpPost(callBackUrl);
		postMethod.setHeader("Content-Type",
				"application/x-www-form-urlencoded");

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("subscription", updateOption));
		nvps.add(new BasicNameValuePair("content", server.json().toJSON(
				edp.getProperties())));
		postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		HttpResponse response = client.execute(postMethod);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			response.getEntity().getContent().close();
			throw new ClientProtocolException(
					"Error in sendind an update to subscriber: " + callBackUrl);
		}
		// read an empty entity and close a connection
		response.getEntity().getContent().close();

	}

	@Override
	public void run() {
		if (callBackUrl != null)
			sendAfterSubscribe();
		else if (edp != null)
			sendAfterPublisherUpdate();
		else if (rssURL != null) {
			sendAfterTopicDelete();
		}
	}

}
