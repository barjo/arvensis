package org.ow2.chameleon.rose.pubsubhubbub.hub.internal;

import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_UPDATE_CONTENT;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
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

	private HttpPost postMethod;

	private JSONService json;
	private LogService logger;

	/**
	 * Main constructor.
	 * 
	 * @param pJson
	 *            {@link JSONService}
	 * @param pLogger
	 *            {@link LogService}
	 */
	public SendSubscription(LogService pLogger, JSONService pJson) {

		this.logger = pLogger;
		this.json = pJson;

		ThreadSafeClientConnManager connManager = new ThreadSafeClientConnManager();
	}

	/**
	 * Send subscriptions to subscribers.
	 * 
	 * @param subscribers
	 *            subscribers url to send notification
	 * @param endpoint
	 *            the {@link EndpointDescription} @EndpointDescription
	 * @param updateOption
	 *            add/remove options
	 */
	public final void sendSubscriptions(final Set<String> subscribers,
			final EndpointDescription endpoint, final String updateOption) {
		// run thread to send subscriptions
		(new SendingUpdateThread(subscribers, endpoint, updateOption)).start();
	}

	/**
	 * Thread to send notifications to subscribers
	 * 
	 * @author Bartek
	 * 
	 */
	private class SendingUpdateThread extends Thread {

		private String updateOption;
		private Set<String> subcribers;
		private EndpointDescription endpoint;

		// client to send notification to subscribers;
		private HttpClient client;
		private ThreadSafeClientConnManager connManager;

		/**
		 * @param pSubscribers
		 *            publishers
		 * @param pEndpoint
		 *            {@link EndpointDescription}
		 * @param pUpdateOption
		 *            new endpoint/remove endpoint
		 */
		public SendingUpdateThread(Set<String> pSubscribers,
				EndpointDescription pEndpoint, String pUpdateOption) {
			super();
			this.subcribers = pSubscribers;
			this.endpoint = pEndpoint;
			this.updateOption = pUpdateOption;

			connManager = new ThreadSafeClientConnManager();
			connManager.setDefaultMaxPerRoute(20);
			connManager.setMaxTotal(200);

			this.client = new DefaultHttpClient(connManager);
		}

		@Override
		public final void run() {

			for (String subscriberUrl : subcribers) {
				postMethod = new HttpPost(subscriberUrl);
				postMethod.setHeader("Content-Type",
						"application/x-www-form-urlencoded");

				final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair(
						HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION, updateOption));
				nvps.add(new BasicNameValuePair(HTTP_POST_UPDATE_CONTENT, json
						.toJSON(this.endpoint.getProperties())));
				try {
					postMethod.setEntity(new UrlEncodedFormEntity(nvps,
							HTTP.UTF_8));
					final HttpResponse response = this.client
							.execute(postMethod);
					if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
						logger.log(LogService.LOG_ERROR,
								"Error in sending an update to subscriber: "
										+ subscriberUrl
										+ ", got response "
										+ response.getStatusLine()
												.getStatusCode());
						response.getEntity().getContent().close();
					}
					// read an empty entity and close a connection
					response.getEntity().getContent().close();

				} catch (Exception e) {
					logger.log(LogService.LOG_ERROR,
							"Error in sending an update to subscriber", e);
				}
				connManager.shutdown();
			}

		}
	}
}
