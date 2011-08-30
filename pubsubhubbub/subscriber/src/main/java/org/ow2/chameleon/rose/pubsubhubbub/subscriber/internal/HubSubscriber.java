package org.ow2.chameleon.rose.pubsubhubbub.subscriber.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import static org.ow2.chameleon.rose.constants.RoseRSSConstants.HubMode;
import static org.ow2.chameleon.rose.constants.RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE;
import static org.ow2.chameleon.rose.constants.RoseRSSConstants.HTTP_POST_HEADER_TYPE;
import static org.ow2.chameleon.rose.constants.RoseRSSConstants.HTTP_POST_PARAMETER_ENDPOINT_FILTER;
import static org.ow2.chameleon.rose.constants.RoseRSSConstants.HTTP_POST_PARAMETER_URL_CALLBACK;


/**
 * Connect and register a subscription to Rose Hub
 * 
 * @author Bartek
 * 
 */
public class HubSubscriber {

	private String urlHub;
	private HttpPost postMethod;
	private HttpClient client;
	private String callBackUrl;
	private String port;

	/**
	 * Register a subscription
	 * 
	 * @param pUrlHub
	 *            url address to Rose Hub, full path
	 * @param callBackUrl
	 *            servlet relative path
	 * @param endpointFilter
	 *            endpoint filter
	 * @param context
	 *            BundleContext
	 * @throws IOException
	 */
	public HubSubscriber(String pUrlHub, String callBackUrl,
			String endpointFilter, BundleContext context) throws IOException {
		this.urlHub = pUrlHub;
		port = (String) context
				.getServiceReference(HttpService.class.getName()).getProperty(
						"org.osgi.service.http.port");
		if (port == null) {
			port = context.getProperty("org.osgi.service.http.port");
		}
		this.callBackUrl = "http://"
				+ InetAddress.getLocalHost().getHostAddress() + ":" + port
				+ callBackUrl;
		client = new DefaultHttpClient();

		postMethod = new HttpPost(this.urlHub);
		postMethod.setHeader("Content-Type", HTTP_POST_HEADER_TYPE);

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
				HubMode.subscribe.toString()));
		nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_ENDPOINT_FILTER,
				endpointFilter));
		nvps.add(new BasicNameValuePair(
				HTTP_POST_PARAMETER_URL_CALLBACK,
				this.callBackUrl));

		postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		HttpResponse response = client.execute(postMethod);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
			response.getEntity().getContent().close();
			throw new ClientProtocolException("Error in subscription");
		}
		// read an empty entity and close a connection
		response.getEntity().getContent().close();
	}

	/**
	 * Sends unsubscription to Rose Hub
	 * 
	 * @throws IOException
	 */
	public void unsubscribe() throws IOException {

		postMethod = new HttpPost(this.urlHub);
		postMethod.setHeader("Content-Type",
				HTTP_POST_HEADER_TYPE);

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair(
				HTTP_POST_PARAMETER_HUB_MODE, HubMode.unsubscribe.toString()));
		nvps.add(new BasicNameValuePair(
				HTTP_POST_PARAMETER_URL_CALLBACK,
				this.callBackUrl));

		postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		HttpResponse response = client.execute(postMethod);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED) {
			response.getEntity().getContent().close();
			throw new ClientProtocolException("Error in unsubscription");
		}
		// read an empty entity and close a connection
		response.getEntity().getContent().close();
	}
}
