package org.ow2.chameleon.rose.pubsubhubbub.client;

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
import org.ow2.chameleon.rose.constants.RoseRSSConstants;

/**Connect and register a subscription to Rose Hub
 * @author Bartek
 *
 */
public class Subscriber {

	private String urlHub;
	private HttpPost postMethod;
	private HttpClient client;
	private String callBackUrl;
	
	/**Register a subscription
	 * @param pUrlHub url address to Rose Hub, full path
	 * @param callBackUrl servlet relative path
	 * @param endpointFilter endpoint filter
	 * @param context BundleContext
	 * @throws IOException
	 */
	public Subscriber(String pUrlHub, String callBackUrl, String endpointFilter,BundleContext context) throws IOException {
		this.urlHub = pUrlHub;
		this.callBackUrl = "http://" + InetAddress.getLocalHost().getHostAddress()
			+ ":" + context.getProperty("org.osgi.service.http.port")
			+callBackUrl;
		client = new DefaultHttpClient();

		postMethod = new HttpPost(this.urlHub);
		postMethod.setHeader("Content-Type",
				"application/x-www-form-urlencoded");

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair(
				RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE, "subscribe"));
		nvps.add(new BasicNameValuePair(
				RoseRSSConstants.HTTP_POST_PARAMETER_ENDPOINT_FILTER, endpointFilter));
		nvps.add(new BasicNameValuePair(
				RoseRSSConstants.HTTP_POST_PARAMETER_URL_CALLBACK, this.callBackUrl));
		
		postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		HttpResponse response = client.execute(postMethod);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED) {
			response.getEntity().getContent().close();
			throw new ClientProtocolException("Error in subscription");
		}
		//read an empty entity and close a connection
		response.getEntity().getContent().close();
	}

	/**Send s unsubscription to Rose Hub
	 * @throws IOException
	 */
	public void unsubscribe() throws IOException{

		postMethod = new HttpPost(this.urlHub);
		postMethod.setHeader("Content-Type",
				"application/x-www-form-urlencoded");

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair(
				RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE, "unsubscribe"));
		nvps.add(new BasicNameValuePair(
				RoseRSSConstants.HTTP_POST_PARAMETER_URL_CALLBACK, this.callBackUrl));
		
		postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		HttpResponse response = client.execute(postMethod);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED) {
			response.getEntity().getContent().close();
			throw new ClientProtocolException("Error in unsubscription");
		}
		//read an empty entity and close a connection
		response.getEntity().getContent().close();	
	}
}
