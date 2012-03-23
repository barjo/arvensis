package org.ow2.chameleon.rose.pubsubhubbub.subscriber.internal;

import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.DEFAULT_HTTP_PORT;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_HEADER_TYPE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_ENDPOINT_FILTER;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_HUB_MODE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_URL_CALLBACK;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_MACHINEID;

import java.io.IOException;
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
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HubMode;

/**
 * Connect and register a subscription to Rose Hub.
 * 
 * @author Bartek
 * 
 */
public class HubSubscriber {

	private final String urlHub;
	private HttpPost postMethod;
	private final HttpClient client;
	private final String callBackUrl;
	private String port;
	private final String host;
	private RoseMachine rose;

	/**
	 * Register a subscription.
	 * 
	 * @param pUrlHub
	 *            url address to Rose Hub, full path
	 * @param pCallBackUrl
	 *            servlet relative path
	 * @param endpointFilter
	 *            endpoint filter
	 * @param context
	 *            BundleContext
	 * @param pRose
	 *            RoseService
	 * @throws IOException
	 *             exception
	 */
	public HubSubscriber(final String pUrlHub, final String pCallBackUrl,
			final String endpointFilter, final BundleContext context,
			final RoseMachine pRose) throws IOException {
		this.urlHub = pUrlHub;
		this.rose=pRose;
		
		final ServiceReference httpServiceRef = context
				.getServiceReference(HttpService.class.getName());
		if (httpServiceRef != null) {
			port = (String) httpServiceRef
					.getProperty("org.osgi.service.http.port");
		}
		if (port == null) {
			port = context.getProperty("org.osgi.service.http.port");
		}

		// set default port number
		if (port == null) {
			port = DEFAULT_HTTP_PORT;
		}

		this.host = pRose.getHost();

		this.callBackUrl = "http://" + this.host + ":" + port + pCallBackUrl;
		client = new DefaultHttpClient();

		postMethod = new HttpPost(this.urlHub);
		postMethod.setHeader("Content-Type", HTTP_POST_HEADER_TYPE);

		final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
				HubMode.subscribe.toString()));
		nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_ENDPOINT_FILTER,
				endpointFilter));
		nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_URL_CALLBACK,
				this.callBackUrl));
		nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_MACHINEID,
				pRose.getId()));

		postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		final HttpResponse response = client.execute(postMethod);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
			response.getEntity().getContent().close();
			throw new ClientProtocolException(
					"Error in subscription, received status from hub: "
							+ response.getStatusLine().getStatusCode());
		}
		// read an empty entity and close a connection
		response.getEntity().getContent().close();
	}

	/**
	 * Sends unsubscription to Rose Hub.
	 * 
	 * @throws IOException
	 *             exception
	 */
	public final void unsubscribe() throws IOException {

		postMethod = new HttpPost(this.urlHub);
		postMethod.setHeader("Content-Type", HTTP_POST_HEADER_TYPE);

		final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
				HubMode.unsubscribe.toString()));
		nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_URL_CALLBACK,
				this.callBackUrl));
		nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_MACHINEID,
				rose.getId()));

		postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		final HttpResponse response = client.execute(postMethod);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_ACCEPTED) {
			response.getEntity().getContent().close();
			throw new ClientProtocolException(
					"Error in unsubscription, received status from hub: "
							+ response.getStatusLine().getStatusCode());
		}
		// read an empty entity and close a connection
		response.getEntity().getContent().close();
	}
}
