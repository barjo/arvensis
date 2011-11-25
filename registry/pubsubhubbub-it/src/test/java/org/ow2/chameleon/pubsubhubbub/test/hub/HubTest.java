package org.ow2.chameleon.pubsubhubbub.test.hub;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.ow2.chameleon.pubsubhubbub.test.clients.AbstractTestConfiguration.waitForIt;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.*;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HubMode;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseEndpointDescription;
import org.ow2.chameleon.rose.pubsubhubbub.hub.Hub;
import org.ow2.chameleon.syndication.FeedEntry;
import org.ow2.chameleon.syndication.FeedReader;
import org.ow2.chameleon.syndication.FeedWriter;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith(JUnit4TestRunner.class)
public class HubTest {

	private static final String HUB_RELATIVE_PATH = "/hub";

	private static final String HUB_INSTANCE_NAME = "Rose_Pubsubhubbub.hub";

	private static final int WAIT_TIME = 100;

	@Inject
	private BundleContext context;

	private HttpService http;

	private OSGiHelper osgi;

	private IPOJOHelper ipojo;

	private String hubUrl;

	private DefaultHttpClient client;

	private int hubResponseCode;

	private JSONService json;

	private List<EndpointDescription> testEndpoints;

	@Before
	public final void setUp() throws UnknownHostException {

		osgi = new OSGiHelper(context);
		ipojo = new IPOJOHelper(context);

		// create a hub instance
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(Hub.INSTANCE_PROPERTY_HUB_URL, HUB_RELATIVE_PATH);
		props.put("instance.name", HUB_INSTANCE_NAME);
		ipojo.createComponentInstance(Hub.COMPONENT_NAME, props);

		initMocks(this);

		hubUrl = "http://" + InetAddress.getLocalHost().getHostName() + ":8080"
				+ HUB_RELATIVE_PATH;

		// get http service
		http = (HttpService) osgi.getServiceObject(HttpService.class.getName(),
				null);
		client = new DefaultHttpClient();

		// get json service
		json = (JSONService) osgi.getServiceObject(JSONService.class.getName(),
				null);

	}

	/**
	 * Global bundle configuration.
	 * 
	 * @return options for paxexam
	 */
	@Configuration
	public static Option[] globalConfigure() {
		Option[] platform = options(felix());

		Option[] bundles = options(provision(
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.ipojo")
						.versionAsInProject(),
				mavenBundle().groupId("org.osgi")
						.artifactId("org.osgi.compendium").versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.testing")
						.artifactId("osgi-helpers").versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.json")
						.artifactId("json-service-json.org")
						.versionAsInProject(),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.http.jetty")
						.versionAsInProject(),
				mavenBundle().groupId("org.apache.httpcomponents")
						.artifactId("httpclient-osgi").versionAsInProject(),
				mavenBundle().groupId("org.apache.httpcomponents")
						.artifactId("httpcore-osgi").versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.rose")
						.artifactId("rose-core").versionAsInProject(),
				mavenBundle().groupId("commons-logging")
						.artifactId("org.ow2.chameleon.commons.logging")
						.versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.syndication")
						.artifactId("syndication-service").versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.syndication")
						.artifactId("rome").versionAsInProject(),
				mavenBundle().groupId("org.jdom")
						.artifactId("com.springsource.org.jdom")
						.versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-api")
						.versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple")
						.versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.rose")
						.artifactId("pubsubhubbub").versionAsInProject(),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.eventadmin")
						.versionAsInProject()

		));

		Option[] r = OptionUtils.combine(platform, bundles);

		return r;
	}

	/**
	 * Mockito bundles.
	 * 
	 * @return options for paxexam contains mockito
	 */
	@Configuration
	public static Option[] mockitoBundle() {
		return options(JUnitOptions.mockitoBundles());
	}

	/**
	 * Check hub instance status.
	 */
	// @Test
	public final void testActivity() {
		// wait for the service to be available.
		waitForIt(WAIT_TIME);
		Assert.assertEquals(ComponentInstance.VALID,
				ipojo.getInstanceByName(HUB_INSTANCE_NAME).getState());

	}

	/**
	 * Publisher send publish and unpublish notification to Hub (without
	 * creating a RSS topic).
	 */
	@Test
	public final void testPublisherConnectNoRSS() {
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");
		publisher.registerPublisher();
		// hub can not register the publisher, because of rss topic not found
		Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, hubResponseCode);
		// stop publisher
		publisher.stop();

	}

	/**
	 * Publisher send publish and unpublish notification to Hub (with creating a
	 * RSS topic).
	 */
	@Test
	public final void testPublisherConnectWithRSS() {
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");
		publisher.createRSSTopic();
		publisher.registerPublisher();

		// hub successfully register a publisher
		Assert.assertEquals(HttpStatus.SC_CREATED, hubResponseCode);
		// stop publisher
		publisher.stop();

	}

	/**
	 * Publisher send update for new endpoint added into RSS topic.
	 */
	@Test
	public final void testPublisherUpdateNewEndpoint() {
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// create publisher
		publisher = new TestPublisher("/rss");
		// create RSS topic
		publisher.createRSSTopic();

		// send publish notification to Hub
		publisher.registerPublisher();

		// prepare endpoints;
		createEndpoints();
		// send feed to topic
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_NEW);
		waitForIt(WAIT_TIME);
		publisher.sendUpdateToHub();
		waitForIt(WAIT_TIME);
		// hub successfully updated
		Assert.assertEquals(HttpStatus.SC_ACCEPTED, hubResponseCode);
		// stop publisher
		publisher.stop();

	}

	/**
	 * Publisher send update for new endpoint added into RSS topic, but no feed
	 * in topic.
	 */
	@Test
	public final void testPublisherUpdateNewEndpointWithoutRSSfeed() {
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();
		// send publish notification to Hub
		publisher.registerPublisher();

		// prepare endpoints;
		createEndpoints();

		waitForIt(WAIT_TIME);
		publisher.sendUpdateToHub();
		waitForIt(WAIT_TIME);

		// hub unsuccessfully updated
		Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, hubResponseCode);
		// stop publisher
		publisher.stop();

	}

	/**
	 * Publisher send update for remove endpoint.
	 */
	@Test
	public final void testPublisherUpdateRemoveEndpoint() {
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_NEW);

		// endpoint add
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_NEW);
		waitForIt(WAIT_TIME);
		publisher.sendUpdateToHub();
		waitForIt(WAIT_TIME);

		// endpoint remove
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_REMOVE);

		// hub unsuccessfully updated removed endpoint
		Assert.assertEquals(HttpStatus.SC_ACCEPTED, hubResponseCode);
		// stop publisher
		publisher.stop();

	}

	/**
	 * Subscriber registrations and unregistration.
	 */
	@Test
	public final void testSubscriber() {
		TestSubscriber subscriber;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// create subscriber
		subscriber = new TestSubscriber("/sub1", "()");

		// start subscriber
		subscriber.start();

		// hub registration response
		Assert.assertEquals(HttpStatus.SC_CREATED, hubResponseCode);

		// stop subscriber
		subscriber.stop();

		// hub uregistration response
		Assert.assertEquals(HttpStatus.SC_ACCEPTED, hubResponseCode);

	}

	/**
	 * Hub sends update (Endpoint added )notification to subscriber. Publisher
	 * shows first.
	 */
	@Test
	public final void testSubscriberUpdateFromHubEndpointAdded() {
		TestSubscriber subscriber;
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_NEW);

		// send update to hub
		publisher.sendUpdateToHub();

		waitForIt(WAIT_TIME);

		// create subscriber
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=*)");

		// start subscriber
		subscriber.start();
		waitForIt(WAIT_TIME);

		// check update notification
		Assert.assertTrue(subscriber.checkUpdate(new EndpointTitle(
				testEndpoints.get(0), HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED)));

		// stop subscriber and publisher
		subscriber.stop();
		publisher.stop();

	}

	/**
	 * Hub sends update (Endpoint added )notification to subscriber. Subscriber
	 * shows first.
	 */
	@Test
	public final void testSubscriberUpdateFromHubEndpointAdded2() {
		TestSubscriber subscriber;
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// create subscriber
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=*)");

		// start subscriber
		subscriber.start();
		waitForIt(WAIT_TIME);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_NEW);

		// send update to hub
		publisher.sendUpdateToHub();

		waitForIt(WAIT_TIME);

		// check update notification
		Assert.assertTrue(subscriber.checkUpdate(new EndpointTitle(
				testEndpoints.get(0), HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED)));
		// stop subscriber and publisher
		subscriber.stop();
		publisher.stop();

	}

	/**
	 * Check subscriber endpoint filter, 2 endpoint are register into hub only
	 * one matches.
	 */
	@Test
	public final void testSubscriberEndpointFilter() {
		TestSubscriber subscriber;
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub (register endpoint id=0)
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_NEW);

		// send update to hub
		publisher.sendUpdateToHub();

		waitForIt(WAIT_TIME);

		// send publish notification to Hub (register endpoint id=1)
		publisher.addRSSFeed(testEndpoints.get(1), FEED_TITLE_NEW);

		// send update to hub
		publisher.sendUpdateToHub();

		waitForIt(WAIT_TIME);

		// create subscriber, try to get only first endpoint
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=0)");

		// start subscriber
		subscriber.start();
		waitForIt(WAIT_TIME);

		// check update notification
		Assert.assertTrue(subscriber.checkUpdate(new EndpointTitle(
				testEndpoints.get(0), HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED)));
		// stop subscriber and publisher
		subscriber.stop();
		publisher.stop();

	}

	/**
	 * Check subscriber endpoint filter, 3 endpoints are register into hub only
	 * 2 matches.
	 */
	@Test
	public final void testSubscriberEndpointComplexFilter() {
		TestSubscriber subscriber;
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub (register endpoint id=0)
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_NEW);

		waitForIt(WAIT_TIME);

		// send update to hub
		publisher.sendUpdateToHub();

		// send publish notification to Hub (register endpoint id=1)
		publisher.addRSSFeed(testEndpoints.get(1), FEED_TITLE_NEW);

		waitForIt(WAIT_TIME);

		// send update to hub
		publisher.sendUpdateToHub();

		// send publish notification to Hub (register endpoint id=2)
		publisher.addRSSFeed(testEndpoints.get(2), FEED_TITLE_NEW);

		waitForIt(WAIT_TIME);

		// send update to hub
		publisher.sendUpdateToHub();

		// create subscriber, try to get only endpoint 0 and 2
		subscriber = new TestSubscriber("/sub1",
				"(|(endpoint.id=0)(endpoint.id=2))");

		// start subscriber
		subscriber.start();
		waitForIt(WAIT_TIME);

		// create expected updates
		List<EndpointTitle> expectUpdates = new ArrayList<EndpointTitle>();
		expectUpdates.add(new EndpointTitle(testEndpoints.get(0),
				HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED));
		expectUpdates.add(new EndpointTitle(testEndpoints.get(2),
				HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED));

		// check expected update and got ones
		Assert.assertTrue(subscriber.checkUpdates(expectUpdates));

		// stop subscriber and publisher
		subscriber.stop();
		publisher.stop();

	}

	/**
	 * Check updates from hub when publisher removes.
	 */
	@Test
	public final void testSubscriberTopicDeleted() {
		TestSubscriber subscriber;
		TestPublisher publisher;
		TestPublisher publisher2;

		// wait for the service to be available.
		waitForIt(WAIT_TIME);

		// prepare endpoints;
		createEndpoints();

		// create publisher1 and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// send publish notification to Hub (register endpoint id=0)
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_NEW);

		waitForIt(WAIT_TIME);

		// send update to hub
		publisher.sendUpdateToHub();

		// send publish notification to Hub (register endpoint id=1)
		publisher.addRSSFeed(testEndpoints.get(1), FEED_TITLE_NEW);

		waitForIt(WAIT_TIME);

		// send update to hub
		publisher.sendUpdateToHub();

		// create publisher2 and send publish notification to Hub
		publisher2 = new TestPublisher("/rss2");

		// create RSS topic
		publisher2.createRSSTopic();

		// send publish notification to Hub (register endpoint id=2)
		publisher2.registerPublisher();
		publisher2.addRSSFeed(testEndpoints.get(2), FEED_TITLE_NEW);

		waitForIt(WAIT_TIME);

		// send update to hub
		publisher2.sendUpdateToHub();

		// create subscriber, try to get only endpoint 0 and 2
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=*)");

		// start subscriber
		subscriber.start();
		waitForIt(WAIT_TIME);

		// publisher2 stops
		publisher.stop();
		waitForIt(WAIT_TIME);
		// create expected updates
		List<EndpointTitle> expectUpdates = new ArrayList<EndpointTitle>();
		expectUpdates.add(new EndpointTitle(testEndpoints.get(0),
				HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED));
		expectUpdates.add(new EndpointTitle(testEndpoints.get(1),
				HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED));
		expectUpdates.add(new EndpointTitle(testEndpoints.get(2),
				HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED));
		expectUpdates.add(new EndpointTitle(testEndpoints.get(0),
				HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED));
		expectUpdates.add(new EndpointTitle(testEndpoints.get(1),
				HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED));
		// check expected update and got ones
		Assert.assertTrue(subscriber.checkUpdates(expectUpdates));

		// stop subscriber and publisher
		subscriber.stop();
		publisher2.stop();

	}

	/**
	 * Fill testEndpoints with simple @EndpointDescription.
	 */
	private void createEndpoints() {
		Map<String, Object> endpProps = new HashMap<String, Object>();
		EndpointDescription endp;
		int index;
		final int endpointsNb = 3;

		testEndpoints = new ArrayList<EndpointDescription>();
		for (index = 0; index < endpointsNb; index++) {
			endpProps.put(OBJECTCLASS, new String[] { "testObject" });
			endpProps.put(ENDPOINT_ID, String.valueOf(index));
			endpProps.put(SERVICE_IMPORTED_CONFIGS,
					new String[] { "import configs" });
			endp = new EndpointDescription(endpProps);
			testEndpoints.add(index, endp);
		}
	}

	/**
	 * Subscriber for testing purpose.
	 * 
	 * @author Bartek
	 * 
	 */
	private final class TestSubscriber extends HttpServlet {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String subscriberRelativeUrl;

		private String filter;
		private String subscriberFullUrl;
		private List<EndpointTitle> postParameters;

		private TestSubscriber(final String pSubscriberRelativeUrl,
				final String pFilter) {
			this.subscriberRelativeUrl = pSubscriberRelativeUrl;
			this.filter = pFilter;
			this.postParameters = new ArrayList<HubTest.EndpointTitle>();
			try {
				// creating full url address to rss topic
				this.subscriberFullUrl = "http://"
						+ InetAddress.getLocalHost().getHostAddress() + ":8080"
						+ pSubscriberRelativeUrl;
			} catch (UnknownHostException e) {
				e.printStackTrace();
				Assert.fail("Subscriber creation error");
			}
		}

		private void start() {
			try {
				http.registerServlet(subscriberRelativeUrl, this, null, null);
				// preparing POST parameters
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
						HubMode.subscribe.toString()));
				nvps.add(new BasicNameValuePair(
						HTTP_POST_PARAMETER_URL_CALLBACK,
						this.subscriberFullUrl));
				nvps.add(new BasicNameValuePair(
						HTTP_POST_PARAMETER_ENDPOINT_FILTER, this.filter));

				sendPOST(nvps);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail();
			}
		}

		private void stop() {
			http.unregister(subscriberRelativeUrl);
			// preparing POST parameters
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
					HubMode.unsubscribe.toString()));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_URL_CALLBACK,
					this.subscriberFullUrl));

			sendPOST(nvps);

		}

		@SuppressWarnings("unchecked")
		@Override
		protected void doPost(final HttpServletRequest req,
				final HttpServletResponse resp) throws ServletException,
				IOException {
			try {
				postParameters.add(new EndpointTitle(RoseEndpointDescription
						.getEndpointDescription(json.fromJSON(req
								.getParameter(HTTP_POST_UPDATE_CONTENT))), req
						.getParameter(HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION)));
			} catch (ParseException e) {
				e.printStackTrace();
				Assert.fail();
			}
			resp.setStatus(HttpStatus.SC_OK);
		}

		/**
		 * Check last update.
		 * 
		 * @param expected
		 *            expected @EndpointDescription with title to check
		 * @return true if expected values are equal
		 */
		private boolean checkUpdate(final EndpointTitle expected) {

			// check if got any update
			Assert.assertEquals("Subscriber didnt get update from hub", 1,
					postParameters.size());
			// check title and content
			if (postParameters.get(0).equals(expected)) {
				return true;
			}

			return false;
		}

		/**
		 * Check list of updates.
		 * 
		 * @param expectUpdates
		 *            list of expected @EndpointDescription with title
		 * 
		 * @return true if and only if expected values are equal
		 */
		private boolean checkUpdates(final List<EndpointTitle> expectUpdates) {

			// check if expected and received are the same, order doesn`t count
			if ((new HashSet<EndpointTitle>(postParameters))
					.equals(new HashSet<EndpointTitle>(expectUpdates))) {
				return true;
			}
			return false;
		}
	}

	private class TestPublisher {

		private String fullRssUrl;
		private String relativeRssUrl;
		private FeedWriter writer;

		public TestPublisher(final String pRelativeRssUrl) {
			this.relativeRssUrl = pRelativeRssUrl;
			try {
				// creating full url address to rss topic
				this.fullRssUrl = "http://"
						+ InetAddress.getLocalHost().getHostAddress() + ":8080"
						+ pRelativeRssUrl;
			} catch (UnknownHostException e) {
				e.printStackTrace();
				Assert.fail("Publisher creation error");
			}
		}

		public void addRSSFeed(final EndpointDescription endpointDescription,
				final String feedTitleNew) {
			FeedEntry feed;

			feed = writer.createFeedEntry();
			feed.title(feedTitleNew);
			feed.content(json.toJSON(endpointDescription.getProperties()));
			try {
				writer.addEntry(feed);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Feed not send");
			}

		}

		private void createRSSTopic() {
			// create RSS topic
			Dictionary<String, String> rssServletProps = new Hashtable<String, String>();
			rssServletProps.put(FeedReader.FEED_TITLE_PROPERTY, "RoseRss");
			rssServletProps.put(
					"org.ow2.chameleon.syndication.feed.servlet.alias",
					relativeRssUrl);
			// get writer service
			ComponentInstance instance = ipojo.createComponentInstance(
					"org.ow2.chameleon.syndication.rome.servlet",
					rssServletProps);
			waitForIt(WAIT_TIME);
			writer = (FeedWriter) osgi.getServiceObject(
					FeedWriter.class.getName(),
					"(instance.name=" + instance.getInstanceName() + ")");

		}

		private void registerPublisher() {
			// preparing POST parameters
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
					HubMode.publish.toString()));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_RSS_TOPIC_URL,
					this.fullRssUrl));

			sendPOST(nvps);
		}

		private void stop() {
			// preparing POST parameters
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
					HubMode.unpublish.toString()));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_RSS_TOPIC_URL,
					this.fullRssUrl));
			sendPOST(nvps);
		}

		private void sendUpdateToHub() {
			// preparing POST parameters
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
					HubMode.update.toString()));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_RSS_TOPIC_URL,
					this.fullRssUrl));

			sendPOST(nvps);
		}

	}

	public final void sendPOST(final List<NameValuePair> nvps) {

		HttpPost postMethod = new HttpPost(hubUrl);
		postMethod.setHeader("Content-Type", HTTP_POST_HEADER_TYPE);

		try {
			postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			HttpResponse response = client.execute(postMethod);
			hubResponseCode = response.getStatusLine().getStatusCode();
			response.getEntity().getContent().close();

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

	/**
	 * Stores Endpoint and Title of update, used in subscriber updates
	 * expectations.
	 * 
	 * @author Bartek
	 * 
	 */
	private static class EndpointTitle {
		private EndpointDescription endp;
		private String title;

		public EndpointTitle(final EndpointDescription pEndp,
				final String pTitle) {
			super();
			this.endp = pEndp;
			this.title = pTitle;
		}

		public EndpointDescription getEndp() {
			return endp;
		}

		public String getTitle() {
			return title;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (obj instanceof EndpointTitle) {
				final EndpointTitle toCheck = (EndpointTitle) obj;
				if (this.getEndp().equals(toCheck.getEndp())
						&& this.getTitle().equals(toCheck.getTitle())) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.getEndp().hashCode() + this.getTitle().hashCode();
		}
	}

}
