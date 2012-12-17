package org.ow2.chameleon.rose.pubsubhubbub.hub.internal;

import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_UPDATE_CONTENT;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
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

	private static int THREAD_POOL = 5;
	private HttpPost postMethod;

	private JSONService json;
	private LogService logger;
	// client to send notification to subscribers;
	private HttpClient client;
	private ThreadSafeClientConnManager connManager;
	private ConcurrentLinkedQueue<String> subscribers;
	private ExecutorService executor;

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

		connManager = new ThreadSafeClientConnManager();
		connManager.setDefaultMaxPerRoute(20);
		connManager.setMaxTotal(200);

		this.client = new DefaultHttpClient(connManager);

		executor = Executors.newFixedThreadPool(THREAD_POOL);
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
	public final void sendSubscriptions(final Set<String> subscribersSet,
			final EndpointDescription endpoint, final String updateOption) {
		synchronized (this) {
			int i = THREAD_POOL;
			subscribers = new ConcurrentLinkedQueue<String>(subscribersSet);

			while (i > 0 && !subscribers.isEmpty()) {
				executor.execute(new SendingUpdateThread(endpoint, updateOption));
				i--;
			}
		}
	}

	/**
	 * Thread to send notifications to subscribers
	 * 
	 * @author Bartek
	 * 
	 */
	private class SendingUpdateThread extends Thread {

		private String updateOption;
		private String subcriber;
		private EndpointDescription endpoint;

		/**
		 * @param pEndpoint
		 *            {@link EndpointDescription}
		 * @param pUpdateOption
		 *            new endpoint/remove endpoint
		 */
		public SendingUpdateThread(EndpointDescription pEndpoint,
				String pUpdateOption) {
			super();
			this.endpoint = pEndpoint;
			this.updateOption = pUpdateOption;

		}

		@Override
		public final void run() {

			while ((subcriber = subscribers.poll()) != null) {
				postMethod = new HttpPost(subcriber);
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
					final HttpResponse response = client.execute(postMethod);
					if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
						logger.log(LogService.LOG_ERROR,
								"Error in sending an update to subscriber: "
										+ subcriber
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
			}

		}
	}
}
