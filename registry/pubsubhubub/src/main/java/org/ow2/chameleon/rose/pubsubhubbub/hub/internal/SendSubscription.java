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


	private String callBackUrl;
	private HttpPost postMethod;
	// client to send notification to subscribers;
	private HttpClient client;
	private JSONService json;
	private LogService logger;

	/**
	 * Main constructor
	 * 
	 * @param pRegistrations
	 *            Endpoints,subscribers,publishers management
	 * @param pJson
	 *            {@link JSONService}
	 * @param pLogger
	 *            {@link LogService}
	 */
	public SendSubscription(LogService pLogger, JSONService pJson){
		this.client = new DefaultHttpClient(new ThreadSafeClientConnManager());
		this.logger = pLogger;
		this.json = pJson;
	}

	/**
	 * Send subscriptions to subscribers
	 */
	
	public void sendSubscriptions(Set<String> subscribers,EndpointDescription endpoint,String updateOption) {
		//run thread to send subscriptions
		(new SendingUpdateThread(subscribers,endpoint,updateOption)).start();
	}

	private class SendingUpdateThread extends Thread {

		private String updateOption;
		private Set<String> subcribers;
		private EndpointDescription endpoint;
		
		public SendingUpdateThread(Set<String> subscribers,EndpointDescription endpoint,String updateOption) {
			super();
			this.subcribers = subscribers;
			this.endpoint = endpoint;
			this.updateOption = updateOption;
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
			

		}
	}
}
