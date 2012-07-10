package org.ow2.chameleon.rose.pubsubhubbub.topic.connector;

import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.DEFAULT_HTTP_PORT;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_HEADER_TYPE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_HUB_MODE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_URL_CALLBACK;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HubMode;

public class HubConnector implements SubscriberConnector, PublisherConnector {

	private Set<String> hubs;
	private String callBack;
	private boolean connectStatus;
	private HttpClient client;
	// to verification purpose, topicUrl + hub.mode
	private ConcurrentHashMap<String, HubMode> verification;

	public static SubscriberConnector getSubscriberConnector(String hubUrl,
			String callBack) {
		HubConnector connector = new HubConnector();
		connector.addHub(hubUrl);
		connector.callBack = callBack;
		connector.connectStatus = false;

		return connector;

	}

	public static HubConnector getPublisherConnector(String hubUrl) {
		HubConnector connector = new HubConnector();
		connector.addHub(hubUrl);
		connector.connectStatus = false;

		return connector;

	}

	public HubConnector() {
		this.hubs = new HashSet<String>();
		this.client = new DefaultHttpClient();
		this.verification = new ConcurrentHashMap<String, HubMode>();
	}

	public void addHub(String hubUrl) {
		synchronized (this) {
			hubs.add(hubUrl);
		}

	}

	public void removeHub(String hubUrl) {
		synchronized (this) {
			hubs.remove(hubUrl);
		}

	}

	public void update(String topic) {
		sendQuery(topic, HubMode.update);
	}

	public boolean connect(String topicUrl) {
		return sendQuery(topicUrl, HubMode.subscribe);

	}

	public void unconnect(String topicUrl) {
		sendQuery(topicUrl, HubMode.unsubscribe);
	}

	private boolean sendQuery(String topicUrl, HubMode hubMode) {

		// TODO work with several hubs ?
		// prepare a POST request method
		for (String hubUrl : hubs) {

			HttpPost postMethod = new HttpPost(hubUrl);

			postMethod.setHeader("Content-Type", HTTP_POST_HEADER_TYPE);
			postMethod.setHeader("User-agent", "RSS pubsubhubbub 0.3");

			final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
					hubMode.toString()));

			// only for subscriber
			if (hubMode == HubMode.subscribe || hubMode == HubMode.unsubscribe) {
				// verification
				verification.put(topicUrl, hubMode);
				// POST parameters
				nvps.add(new BasicNameValuePair(
						HTTP_POST_PARAMETER_URL_CALLBACK, callBack));
				nvps.add(new BasicNameValuePair(
						HTTP_POST_PARAMETER_RSS_TOPIC_URL, topicUrl));
				nvps.add(new BasicNameValuePair("hub.verify", "sync"));
			}

			try {
				postMethod
						.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
				final HttpResponse response = client.execute(postMethod);
				// 204 is OK
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
					response.getEntity().getContent().close();
					throw new ClientProtocolException(
							"Error in subscription, received status from hub: "
									+ response.getStatusLine().getStatusCode());
				}
				connectStatus = true;

			} catch (IOException e) {
				e.printStackTrace();
				connectStatus = false;
			}
		}

		return connectStatus;
	}

	// check if expecting verification
	public boolean checkPending(String topicUrl, HubMode hubMode) {
		if (topicUrl == null || hubMode == null) {
			return false;
		}
		synchronized (this) {
			if (verification.containsKey(topicUrl)
					&& verification.get(topicUrl) == hubMode) {
				verification.remove(topicUrl);
				return true;
			}
			return false;
		}

	}
	
	public static String findPort(BundleContext context) {
		String port = null;
		// get a port number
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
		return port;
	}

}
