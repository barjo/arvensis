package org.ow2.chameleon.rose.pubsubhubbub.topic.subscriber.internal;

import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_HUB_MODE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HubMode;
import org.ow2.chameleon.rose.pubsubhubbub.topic.connector.HubConnector;
import org.ow2.chameleon.rose.pubsubhubbub.topic.connector.SubscriberConnector;
import org.ow2.chameleon.rose.pubsubhubbub.topic.subscriber.Subscription;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

@Component(name = "SubscriberManager")
public class SubscriberManager extends HttpServlet implements
		ServiceTrackerCustomizer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Property(name = "hub.url", mandatory = true)
	private String hubUrl;

	@Property(name = "call.back", mandatory = true)
	private String callBack;

	@Requires
	private LogService log;

	@Requires
	private HttpService httpService;

	@Requires
	private RoseMachine rose;

	// subsciber + topic url
	private Map<Subscription, String> subscription;
	private BundleContext context;
	private ServiceTracker tracker;
	private SubscriberConnector hubConnect;
	private String callbackFullURI;

	public SubscriberManager(BundleContext context) {
		super();
		this.context = context;
	}

	@Validate
	void start() throws ServletException, NamespaceException {

		// register servlet in order to retrieve feeds and verifications from
		// hub
		httpService.registerServlet(callBack, this, null, null);

		// prepare hub connector
		callbackFullURI = "http://"
				+ rose.getHost() + ":" + HubConnector.findPort(context) + callBack;
		hubConnect = HubConnector.getSubscriberConnector(hubUrl, callbackFullURI);

		subscription = new HashMap<Subscription, String>();

		// run subscription tracker
		tracker = new ServiceTracker(context, Subscription.class.getName(),
				this);
		tracker.open();

	}

	@Invalidate
	void stop() {
		tracker.close();
		httpService.unregister(callBack);
	}

	public Object addingService(ServiceReference reference) {
		Subscription subs = (Subscription) context.getService(reference);

		// store a subscriber
		subscription.put(subs, subs.getTopicUrl());

		if (hubConnect.connect(subs.getTopicUrl()) == false) {
			log.log(LogService.LOG_ERROR, "Unsuccessful connect to hub "
					+ hubUrl);
		}
		return subs;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
		Subscription subs = (Subscription) service;

		hubConnect.unconnect(subscription.get(subs));
		subscription.values().remove(subs);

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		InputStream in = req.getInputStream();
		SyndFeedInput input = new SyndFeedInput();

		ClassLoader bundle = this.getClass().getClassLoader();
		ClassLoader thread = Thread.currentThread().getContextClassLoader();
		try {
			// Switch
			Thread.currentThread().setContextClassLoader(bundle);
			SyndFeed feed = input.build(new XmlReader(in));
			notifySubscriptions(feed);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Restore
			Thread.currentThread().setContextClassLoader(thread);
		}

	}

	private void notifySubscriptions(SyndFeed feed) {
		for (Entry<Subscription, String> sub : subscription.entrySet()) {

			if (sub.getValue().equals(feed.getLink())
					|| sub.getValue().equals("http://" + feed.getLink())) {
				sub.getKey().onContent((SyndEntry)feed.getEntries().get(0));
			}
		}
	}

	// for hub verification purpose
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		int respStatus = HttpServletResponse.SC_BAD_REQUEST;

		// retrieve parameters
		String hubMode = req.getParameter(HTTP_POST_PARAMETER_HUB_MODE);
		String topicUrl = req.getParameter(HTTP_POST_PARAMETER_RSS_TOPIC_URL);
		String challenge = req.getParameter("hub.challenge");

		// check request parameters
		if (hubConnect.checkPending(topicUrl, HubMode.valueOf(hubMode))) {
			resp.getWriter().append(challenge);
			respStatus = HttpServletResponse.SC_OK;
		}

		resp.setStatus(respStatus);
	}
}
