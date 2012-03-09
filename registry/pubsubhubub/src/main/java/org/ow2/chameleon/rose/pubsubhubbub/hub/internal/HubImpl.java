package org.ow2.chameleon.rose.pubsubhubbub.hub.internal;

import static org.osgi.service.log.LogService.LOG_ERROR;
import static org.osgi.service.log.LogService.LOG_INFO;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.FEED_TITLE_NEW;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.FEED_TITLE_REMOVE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_HEADER_TYPE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_ENDPOINT_FILTER;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_HUB_MODE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_MACHINEID;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_URL_CALLBACK;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED;
import static org.ow2.chameleon.rose.pubsubhubbub.hub.Hub.COMPONENT_NAME;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HubMode;
import org.ow2.chameleon.rose.pubsubhubbub.hub.Hub;
import org.ow2.chameleon.rose.util.DefaultLogService;
import org.ow2.chameleon.syndication.FeedEntry;
import org.ow2.chameleon.syndication.FeedReader;

/**
 * Component class to work as Hub in Pubsubhubbub technology, specially modified
 * to work with Rose.
 * 
 * @author Bartek
 * 
 */
@Component(name = COMPONENT_NAME)
@Provides
public class HubImpl extends HttpServlet implements Hub {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1526708334275691196L;

	private static final String FEED_READER_FACTORY_FILTER = "(&("
			+ Constants.OBJECTCLASS
			+ "=org.apache.felix.ipojo.Factory)(factory.name=org.ow2.chameleon.syndication.rome.reader))";
	private static final String READER_SERVICE_CLASS = "org.ow2.chameleon.syndication.FeedReader";

	private static final int FEED_PERIOD = 10;

	private static final boolean FEED_POLLING = false;

	@Requires
	private transient HttpService httpService;

	@Requires
	private transient JSONService json;

	@Requires(optional = true, defaultimplementation = DefaultLogService.class)
	private transient LogService logger;

	@Requires(filter = FEED_READER_FACTORY_FILTER)
	private Factory feedReaderFactory;

	@Property(name = INSTANCE_PROPERTY_HUB_URL, mandatory = true)
	private String hubServlet;

	// HTTP response status code
	private int responseCode;

	// store instances of RSS reader for different topics
	private Map<Object, ReaderWithFeedIndex> readers;
	private Dictionary<String, Object> instanceDictionary;
	private transient ServiceTracker feedReaderTracker;
	private transient BundleContext context;
	private transient RegistrationsImpl registrations;

	public HubImpl(final BundleContext pContext) {
		this.context = pContext;
	}

	@Validate
	public final void start() {
		try {
			httpService.registerServlet(hubServlet, this, null, null);
			readers = new HashMap<Object, ReaderWithFeedIndex>();
			registrations = new RegistrationsImpl(json, logger);

			// start tracking for all feed readers
			feedReaderTracker = new ServiceTracker(context,
					FeedReader.class.getName(), new FeedReaderTracker());
			feedReaderTracker.open();
			logger.log(LOG_INFO, "Pubsubhubbub successfully starts");

		} catch (Exception e) {
			logger.log(LOG_ERROR, "Error in starting a hub", e);
		}
	}

	@Invalidate
	public final void stop() {
		feedReaderTracker.close();
		// unregister hubServlet (subscriber/publisher purpose)
		httpService.unregister(hubServlet);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest
	 * , javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected final void doPost(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {

		String rssUrl;
		String endpointFilter;
		String callBackUrl;
		String machineID;
		FeedEntry feed;
		int feedIndex = 0;
		int feedIndexDigit = 0;

		// check the content type, must be application/x-www-form-urlencoded
		if ((!(req.getHeader("Content-Type").equals(HTTP_POST_HEADER_TYPE)))
				|| (req.getParameter(HTTP_POST_PARAMETER_HUB_MODE) == null)) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			return;
		}
		rssUrl = req.getParameter(HTTP_POST_PARAMETER_RSS_TOPIC_URL);
		endpointFilter = req.getParameter(HTTP_POST_PARAMETER_ENDPOINT_FILTER);
		callBackUrl = req.getParameter(HTTP_POST_PARAMETER_URL_CALLBACK);
		machineID = req.getParameter(HTTP_POST_PARAMETER_MACHINEID);
		// check the hub mode
		switch (HubMode.valueOf(req.getParameter(HTTP_POST_PARAMETER_HUB_MODE))) {
		case publish:

			if ((rssUrl != null) && (machineID != null)
					&& (createReader(rssUrl))) {
				registrations.addTopic(rssUrl, machineID);
				responseCode = HttpStatus.SC_CREATED;
				logger.log(LOG_INFO, "Successfully register publisher from: "
						+ rssUrl);
			} else {
				responseCode = HttpStatus.SC_BAD_REQUEST;
			}
			break;

		case unpublish:

			if (rssUrl != null) {
				// remove a topic
				registrations.removeTopic(rssUrl);
				readers.remove(rssUrl);
				responseCode = HttpStatus.SC_ACCEPTED;
				logger.log(LOG_INFO, "Successfully removed publisher from: "
						+ rssUrl);
			} else {
				responseCode = HttpStatus.SC_BAD_REQUEST;
			}
			break;

		case update:
			if ((rssUrl == null) || (readers.get(rssUrl) == null)) {
				responseCode = HttpStatus.SC_BAD_REQUEST;
				break;
			}
			feed = readers.get(rssUrl).getFeedReader().getLastEntry();
			if (feed == null) {
				responseCode = HttpStatus.SC_BAD_REQUEST;
				break;
			}
			// retrieve feed index from feed content
			feedIndexDigit = getFeedIndexDigit(feed.content());
			feedIndex = getFeedIndex(feed.content(), feedIndexDigit);
			while (readers.get(rssUrl).getFeedIndex() != feedIndex) {
				try {
					Thread.sleep(10);
					// try to get newest feed
					feed = readers.get(rssUrl).getFeedReader().getLastEntry();
					// update index from new feed
					feedIndexDigit = getFeedIndexDigit(feed.content());
					feedIndex = getFeedIndex(feed.content(), feedIndexDigit);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			try {
				@SuppressWarnings("unchecked")
				EndpointDescription edp = getEndpointDescriptionFromJSON(json
						.fromJSON(feed.content().substring(feedIndexDigit + 1,
								feed.content().length())));
				logger.log(LOG_INFO, "Received update from " + rssUrl + ", "
						+ feed.title() + " : " + edp);
				if (feed.title().equals(FEED_TITLE_NEW)) {
					registrations.addEndpointByTopicRssUrl(rssUrl, edp);
				} else if (feed.title().equals(FEED_TITLE_REMOVE)) {
					registrations.removeEndpointByTopicRssUrl(rssUrl, edp);
				}
				readers.get(rssUrl).increaseIndex();
				responseCode = HttpStatus.SC_ACCEPTED;
			} catch (ParseException e) {
				responseCode = HttpStatus.SC_BAD_REQUEST;
				logger.log(LOG_ERROR, "Update false", e);
			}
			break;

		case subscribe:
			if ((endpointFilter == null) || (callBackUrl == null)) {
				responseCode = HttpStatus.SC_BAD_REQUEST;
			} else {
				registrations.addSubscriber(callBackUrl, endpointFilter);
				responseCode = HttpStatus.SC_CREATED;
				logger.log(LOG_INFO, "Successfully register subscriber from  "
						+ callBackUrl + "with filer: " + endpointFilter);
			}

			break;

		case unsubscribe:
			if (callBackUrl == null) {
				responseCode = HttpStatus.SC_BAD_REQUEST;
				break;
			}
			registrations.removeSubscriber(callBackUrl);
			responseCode = HttpStatus.SC_ACCEPTED;
			logger.log(LOG_INFO, "Successfully removed subscriber from  "
					+ callBackUrl);

			break;

		case getAllEndpoints:
			// for Rose Pubsuhhubbub webconsole purpose
			resp.setContentType("text/html");
			for (EndpointDescription endpoint : registrations.getAllEndpoints()
					.keySet()) {
				resp.getWriter().append(endpoint.toString() + "<br><br>");
			}
			responseCode = HttpStatus.SC_ACCEPTED;
			break;

		// hub.mode not found
		default:
			responseCode = HttpStatus.SC_BAD_REQUEST;
			break;
		}
		resp.setStatus(responseCode);
	}

	/**
	 * Run trackers for Feed readers and Feed read factories.
	 * 
	 * @param rssUrl
	 *            url address to read feeds
	 * @return true if successfully found a feed reader
	 */
	private boolean createReader(final String rssUrl) {

		if (readers.containsKey(rssUrl)) {
			return true;
		}

		// set reader instance filter
		String readerFilter = ("(&(" + Constants.OBJECTCLASS + "="
				+ READER_SERVICE_CLASS + ")(" + FeedReader.FEED_URL_PROPERTY
				+ "=" + rssUrl + "))");
		try {

			ServiceReference[] sref = context.getServiceReferences(
					FeedReader.class.getName(), readerFilter);
			// check if reader is already available
			if (sref == null) {

				// create instance
				instanceDictionary = new Hashtable<String, Object>();
				instanceDictionary.put("feed.url", rssUrl);
				// instanceDictionary.put("feed.period", FEED_PERIOD);
				instanceDictionary.put("feed.pooling", FEED_POLLING);

				feedReaderFactory.createComponentInstance(instanceDictionary);
				sref = context.getServiceReferences(READER_SERVICE_CLASS,
						readerFilter);

			}
			// store reader
			readers.put(
					rssUrl,
					new ReaderWithFeedIndex((FeedReader) context
							.getService(sref[0])));

			// release reference
			context.ungetService(sref[0]);

		} catch (Exception e) {
			logger.log(LOG_ERROR, "Can not create reader for " + rssUrl, e);
			return false;
		}

		return true;
	}

	/**
	 * Creates an EndpointDescription from JSON map, checks for errors which
	 * occurs after parsing from string to JSON.
	 * 
	 * @param map
	 *            endpoint description map property
	 * @return proper endpoint description
	 */
	@SuppressWarnings("unchecked")
	private EndpointDescription getEndpointDescriptionFromJSON(
			final Map<String, Object> map) {

		if (map.get(Constants.OBJECTCLASS) instanceof ArrayList<?>) {
			map.put(Constants.OBJECTCLASS, ((ArrayList<String>) map
					.get(Constants.OBJECTCLASS)).toArray(new String[0]));
		}

		if (map.get(RemoteConstants.ENDPOINT_SERVICE_ID) instanceof Integer) {
			Integer id = (Integer) map
					.get((RemoteConstants.ENDPOINT_SERVICE_ID));
			map.put(RemoteConstants.ENDPOINT_SERVICE_ID, id.longValue());
		}
		return new EndpointDescription(map);
	}

	/**
	 * Gets feed index digit from feed content.
	 * 
	 * @param pContent
	 *            feed content
	 * @return number of digits in index
	 */
	private int getFeedIndexDigit(String pContent) {
		return Integer.parseInt(pContent.substring(0, 1));

	}

	/**
	 * Gets feed index from feed content.
	 * 
	 * @param pContent
	 *            feed content
	 * @param pFeedIndexDigit
	 *            number of digits in index number
	 * @return feed index
	 */
	private int getFeedIndex(String pContent, int pFeedIndexDigit) {
		return Integer.parseInt(pContent.substring(1, pFeedIndexDigit + 1));
	}

	public RegistrationsImpl getRegistrations() {
		return registrations;
	}

	/**
	 * Tracker for Feed reader.
	 * 
	 * @author Bartek
	 * 
	 */
	private class FeedReaderTracker implements ServiceTrackerCustomizer {

		public Object addingService(final ServiceReference reference) {
			FeedReader reader = (FeedReader) context.getService(reference);
			if (!(readers.containsKey(reference
					.getProperty(FeedReader.FEED_URL_PROPERTY)))) {
				readers.put(
						reference.getProperty(FeedReader.FEED_URL_PROPERTY),
						new ReaderWithFeedIndex(reader));
			}
			return reader;
		}

		public void modifiedService(final ServiceReference reference,
				final Object service) {

		}

		public void removedService(final ServiceReference reference,
				final Object service) {
			readers.remove(reference.getProperty(FeedReader.FEED_URL_PROPERTY));

		}
	}

	/**
	 * Contains FeedREader with feed index.
	 * 
	 * @author Bartek
	 * 
	 */
	private class ReaderWithFeedIndex {
		private FeedReader feedReader;
		private int feedIndex;

		public ReaderWithFeedIndex(FeedReader feedReader) {
			this.feedReader = feedReader;
			feedIndex = 1;
		}

		public FeedReader getFeedReader() {
			return feedReader;
		}

		public int getFeedIndex() {
			return feedIndex;
		}

		public void increaseIndex() {
			feedIndex++;
		}
	}
}
