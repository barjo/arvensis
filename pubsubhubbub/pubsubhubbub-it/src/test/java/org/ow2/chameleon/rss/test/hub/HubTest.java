package org.ow2.chameleon.rss.test.hub;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.ow2.chameleon.rose.constants.RoseRSSConstants.*;
import static org.ow2.chameleon.rss.test.clients.AbstractTestConfiguration.waitForIt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
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
import org.ow2.chameleon.rose.constants.RoseRSSConstants;
import org.ow2.chameleon.rose.constants.RoseRSSConstants.HubMode;
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

	@Inject
	protected BundleContext context;

	protected HttpService http;

	protected OSGiHelper osgi;

	protected IPOJOHelper ipojo;

	private String hubUrl;

	private DefaultHttpClient client;

	private int hubResponseCode;

	private JSONService json;

	private List<EndpointDescription> testEndpoints;

	@Before
	public void setUp() throws UnknownHostException {

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
	 * Global bundle configuration
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
				mavenBundle().groupId("org.ow2.chameleon.rss")
						.artifactId("pubsubhubbub-hub").versionAsInProject(),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.eventadmin")
						.versionAsInProject()

		));

		Option[] r = OptionUtils.combine(platform, bundles);

		return r;
	}

	/**
	 * Mockito bundles
	 */
	@Configuration
	public static Option[] mockitoBundle() {
		return options(JUnitOptions.mockitoBundles());
	}

	/**
	 * Check hub instance status
	 */
	// @Test
	public void testActivity() {
		// wait for the service to be available.
		waitForIt(100);
		Assert.assertEquals(ComponentInstance.VALID,
				ipojo.getInstanceByName(HUB_INSTANCE_NAME).getState());

	}

	/**
	 * Publisher send publish and unpublish notification to Hub (without
	 * creating a RSS topic)
	 */
	// @Test
	public void testPublisherConnectNoRSS() {
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(100);

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
	 * RSS topic)
	 */
	// @Test
	public void testPublisherConnectWithRSS() {
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(100);

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
	 * Publisher send update for new endpoint added into RSS topic
	 */
	// @Test
	public void testPublisherUpdateNewEndpoint() {
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(100);

		// create publisher
		publisher = new TestPublisher("/rss");
		// create RSS topic
		publisher.createRSSTopic();

		// send publish notification to Hub
		publisher.registerPublisher();

		// prepare endpoints;
		createEndpoints();
		// send feed to topic
		publisher.addRSSFeed(testEndpoints.get(0),
				RoseRSSConstants.FEED_TITLE_NEW);
		waitForIt(100);
		publisher.sendUpdateToHub();
		waitForIt(100);
		// hub successfully updated
		Assert.assertEquals(HttpStatus.SC_ACCEPTED, hubResponseCode);
		// stop publisher
		publisher.stop();

	}

	/**
	 * Publisher send update for new endpoint added into RSS topic, but no feed
	 * in topic
	 */
	// @Test
	public void testPublisherUpdateNewEndpointWithoutRSSfeed() {
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(100);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();
		// send publish notification to Hub
		publisher.registerPublisher();

		// prepare endpoints;
		createEndpoints();

		waitForIt(100);
		publisher.sendUpdateToHub();
		waitForIt(100);

		// hub unsuccessfully updated
		Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, hubResponseCode);
		// stop publisher
		publisher.stop();

	}

	/**
	 * Publisher send update for remove endpoint,
	 */
	// @Test
	public void testPublisherUpdateRemoveEndpoint() {
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(100);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0),
				RoseRSSConstants.FEED_TITLE_NEW);

		// endpoint add
		publisher.addRSSFeed(testEndpoints.get(0),
				RoseRSSConstants.FEED_TITLE_NEW);
		waitForIt(100);
		publisher.sendUpdateToHub();
		waitForIt(100);

		// endpoint remove
		publisher.addRSSFeed(testEndpoints.get(0),
				RoseRSSConstants.FEED_TITLE_REMOVE);

		// hub unsuccessfully updated removed endpoint
		Assert.assertEquals(HttpStatus.SC_ACCEPTED, hubResponseCode);
		// stop publisher
		publisher.stop();

	}

	/**
	 * Subscriber registrations and unregistration
	 */
	// @Test
	public void testSubscriber() {
		TestSubscriber subscriber;

		// wait for the service to be available.
		waitForIt(100);

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
	 * shows first
	 */
	// @Test
	public void testSubscriberUpdateFromHubEndpointAdded() {
		TestSubscriber subscriber;
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(100);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0),
				RoseRSSConstants.FEED_TITLE_NEW);

		// send update to hub
		publisher.sendUpdateToHub();

		waitForIt(100);

		// create subscriber
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=*)");

		// start subscriber
		subscriber.start();
		waitForIt(100);

		// check update notification
		Assert.assertTrue(subscriber.checkUpdate(new EndpointTitle(
				testEndpoints.get(0), HUB_UPDATE_ENDPOINT_ADDED)));

		// stop subscriber and publisher
		subscriber.stop();
		publisher.stop();

	}

	/**
	 * Hub sends update (Endpoint added )notification to subscriber. Subscriber
	 * shows first
	 */
	// @Test
	public void testSubscriberUpdateFromHubEndpointAdded2() {
		TestSubscriber subscriber;
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(100);

		// create subscriber
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=*)");

		// start subscriber
		subscriber.start();
		waitForIt(100);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0),
				RoseRSSConstants.FEED_TITLE_NEW);

		// send update to hub
		publisher.sendUpdateToHub();

		waitForIt(100);

		// check update notification
		Assert.assertTrue(subscriber.checkUpdate(new EndpointTitle(
				testEndpoints.get(0), HUB_UPDATE_ENDPOINT_ADDED)));
		// stop subscriber and publisher
		subscriber.stop();
		publisher.stop();

	}

	/**
	 * Check subscriber endpoint filter, 2 endpoint are register into hub only
	 * one matches
	 */
//	@Test
	public void testSubscriberEndpointFilter() {
		TestSubscriber subscriber;
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(100);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub (register endpoint id=0)
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0),
				RoseRSSConstants.FEED_TITLE_NEW);

		// send update to hub
		publisher.sendUpdateToHub();

		waitForIt(100);

		// send publish notification to Hub (register endpoint id=1)
		publisher.addRSSFeed(testEndpoints.get(1),
				RoseRSSConstants.FEED_TITLE_NEW);

		// send update to hub
		publisher.sendUpdateToHub();

		waitForIt(100);

		// create subscriber, try to get only first endpoint
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=0)");

		// start subscriber
		subscriber.start();
		waitForIt(100);

		// check update notification
		Assert.assertTrue(subscriber.checkUpdate(new EndpointTitle(
				testEndpoints.get(0), HUB_UPDATE_ENDPOINT_ADDED)));
		// stop subscriber and publisher
		subscriber.stop();
		publisher.stop();

	}

	/**
	 * Check subscriber endpoint filter, 3 endpoints are register into hub only
	 * 2 matches
	 */
	 @Test
	public void testSubscriberEndpointComplexFilter() {
		TestSubscriber subscriber;
		TestPublisher publisher;

		// wait for the service to be available.
		waitForIt(100);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub (register endpoint id=0)
		publisher.registerPublisher();
		publisher.addRSSFeed(testEndpoints.get(0),
				RoseRSSConstants.FEED_TITLE_NEW);

		waitForIt(100);
		// send update to hub
		publisher.sendUpdateToHub();

		// send publish notification to Hub (register endpoint id=1)
		publisher.addRSSFeed(testEndpoints.get(1),
				RoseRSSConstants.FEED_TITLE_NEW);

		waitForIt(100);
		// send update to hub
		publisher.sendUpdateToHub();

		// send publish notification to Hub (register endpoint id=2)
		publisher.addRSSFeed(testEndpoints.get(2),
				RoseRSSConstants.FEED_TITLE_NEW);

		waitForIt(100);
		// send update to hub
		publisher.sendUpdateToHub();

		// create subscriber, try to get only endpoint 0 and 2
		subscriber = new TestSubscriber("/sub1",
				"(|(endpoint.id=0)(endpoint.id=2))");

		// start subscriber
		subscriber.start();
		waitForIt(100);

		// create expected updates
		List<EndpointTitle> expectUpdates = new ArrayList<EndpointTitle>();
		expectUpdates.add(new EndpointTitle(testEndpoints.get(0),
				HUB_UPDATE_ENDPOINT_ADDED));
		expectUpdates.add(new EndpointTitle(testEndpoints.get(2),
				HUB_UPDATE_ENDPOINT_ADDED));
		// check expected update and got ones
		Assert.assertTrue(subscriber.checkUpdates(expectUpdates));
		// stop subscriber and publisher
		subscriber.stop();
		publisher.stop();

	}

	private void createEndpoints() {
		Map<String, Object> endpProps = new HashMap<String, Object>();
		EndpointDescription endp;
		int index;

		testEndpoints = new ArrayList<EndpointDescription>();
		for (index = 0; index < 3; index++) {
			endpProps.put(OBJECTCLASS, new String[] { "testObject" });
			endpProps.put(ENDPOINT_ID, String.valueOf(index));
			endpProps.put(SERVICE_IMPORTED_CONFIGS,
					new String[] { "import configs" });
			endp = new EndpointDescription(endpProps);
			testEndpoints.add(index, endp);
		}
	}

	private class TestSubscriber extends HttpServlet {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String subscriberRelativeUrl;

		private String filter;
		private String subscriberFullUrl;
		private List<EndpointTitle> postParameters;
		

		private TestSubscriber(String subscriberRelativeUrl, String filter) {
			this.subscriberRelativeUrl = subscriberRelativeUrl;
			this.filter = filter;
			this.postParameters = new ArrayList<HubTest.EndpointTitle>();
			try {
				// creating full url address to rss topic
				this.subscriberFullUrl = "http://"
						+ InetAddress.getLocalHost().getHostAddress() + ":8080"
						+ subscriberRelativeUrl;
			} catch (UnknownHostException e) {
				e.printStackTrace();
				Assert.fail("Subscriber creation error");
			}
		}

		void start() {
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

		void stop() {
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
		protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			System.out.println("got something");
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
		 * Check last update
		 * 
		 * @param endp
		 * @param title
		 * @return
		 */
		@SuppressWarnings("unchecked")
		boolean checkUpdate(EndpointTitle expected) {

			// check if got any update
			Assert.assertEquals("Subscriber didnt got update from hub", 1,
					postParameters.size());
			// check title and content
			if (postParameters.get(0).equals(expected)) {
				return true;
			}

			return false;
		}

		/**
		 * Check list of updates
		 * 
		 * @param expectUpdates
		 * 
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public boolean checkUpdates(List<EndpointTitle> expectUpdates) {
			int updateMatched = 0;
			
			// check the length of expected and got parameters
			Assert.assertEquals(expectUpdates.size(),
					postParameters.size());
			
			// remove expected and got parameters if are the same
			for (EndpointTitle endpointTitle : postParameters) {

				for (EndpointTitle endpointTitleExpected : expectUpdates) {
					if (endpointTitle.equals(endpointTitleExpected)) {
						System.out.println("removed");
						updateMatched++;

					}

				}
			}
			// check if any expect update or post parameter remains
			if (updateMatched == expectUpdates.size()) {
				return true;
			}

			return false;
		}
	}

	private class TestPublisher {

		private String fullRssUrl;
		private String relativeRssUrl;
		private FeedWriter writer;

		public TestPublisher(String relativeRssUrl) {
			this.relativeRssUrl = relativeRssUrl;
			try {
				// creating full url address to rss topic
				this.fullRssUrl = "http://"
						+ InetAddress.getLocalHost().getHostAddress() + ":8080"
						+ relativeRssUrl;
			} catch (UnknownHostException e) {
				e.printStackTrace();
				Assert.fail("Publisher creation error");
			}
		}

		public void addRSSFeed(EndpointDescription endpointDescription,
				String feedTitleNew) {
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

		void createRSSTopic() {
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
			waitForIt(100);
			writer = (FeedWriter) osgi.getServiceObject(
					FeedWriter.class.getName(),
					"(instance.name=" + instance.getInstanceName() + ")");

		}

		void registerPublisher() {
			// preparing POST parameters
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
					HubMode.publish.toString()));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_RSS_TOPIC_URL,
					this.fullRssUrl));

			sendPOST(nvps);
		}

		void stop() {
			// preparing POST parameters
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
					HubMode.unpublish.toString()));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_RSS_TOPIC_URL,
					this.fullRssUrl));
			sendPOST(nvps);
		}

		void sendUpdateToHub() {
			// preparing POST parameters
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
					HubMode.update.toString()));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_RSS_TOPIC_URL,
					this.fullRssUrl));

			sendPOST(nvps);
		}

	}

	public void sendPOST(List<NameValuePair> nvps) {

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
	 * Stores Endpoint and Title of update, used in subscriber updates expection
	 * 
	 * @author Bartek
	 * 
	 */
	private class EndpointTitle {
		private EndpointDescription endp;
		private String title;

		public EndpointTitle(EndpointDescription endp, String title) {
			super();
			this.endp = endp;
			this.title = title;
		}

		public EndpointDescription getEndp() {
			return endp;
		}

		public String getTitle() {
			return title;
		}

		@Override
		public boolean equals(Object obj) {

			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (obj instanceof EndpointTitle) {
				final EndpointTitle toCheck = (EndpointTitle) obj;
				if (this.getEndp().equals(toCheck.getEndp())
						&& this.getTitle().equals(toCheck.getTitle())) {
					return true;
				}
			}
			return false;
		}
	}

}
