package org.ow2.chameleon.rss.test;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;

import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseEndpointDescription;
import org.ow2.chameleon.rose.constants.RoseRSSConstants;

import java.io.IOException;
import java.text.ParseException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.After;
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
import org.osgi.service.http.NamespaceException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.ow2.chameleon.syndication.FeedEntry;
import org.ow2.chameleon.syndication.FeedReader;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith(JUnit4TestRunner.class)
public class PublisherTest {

	private static final String PUBLISHER_INSTANCE_NAME = "Rose_Pubsubhubbub.publisher-1";

	protected HttpService http;

	@Inject
	protected BundleContext context;

	private OSGiHelper osgi;

	private IPOJOHelper ipojo;

	private TestHub hub;

	private EndpointDescription endp;

	private FeedReader reader;

	@Before
	public void setUp() {
		osgi = new OSGiHelper(context);
		ipojo = new IPOJOHelper(context);
		initMocks(this);

		// gathering a http service
		http = (HttpService) osgi.getServiceObject(HttpService.class.getName(),
				null);
		// run a test hub, first response status accepted for registration a
		// tested publisher
		hub = new TestHub(http, HttpStatus.SC_CREATED);
		hub.start();

		// create publisher instance, register publisher in hub
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("hub.url", "http://localhost:8080/hub");
		props.put("rss.url", "/roserss");
		props.put("instance.name", PUBLISHER_INSTANCE_NAME);
		ipojo.createComponentInstance("Rose_Pubsubhubbub.publisher", props);

		// change hub response status
		hub.changeResponseStatus(HttpStatus.SC_ACCEPTED);

		// prepare test endpoint description
		Map<String, Object> endpProps = new HashMap<String, Object>();
		endpProps.put(OBJECTCLASS, new String[] { "testObject" });
		endpProps.put(ENDPOINT_ID, "1");
		endpProps.put(SERVICE_IMPORTED_CONFIGS,
				new String[] { "import configs" });
		endp = new EndpointDescription(endpProps);

		// prepare rss feed reader, created by publisher
		reader = (FeedReader) osgi.getServiceObject(FeedReader.class.getName(),
				null);

	}

	@After
	public void tearDown() {

		// response to unregister publisher
		System.out.println("ending");
		hub.changeResponseStatus(HttpStatus.SC_ACCEPTED);
		// stop publisher, waits for response from test hub
		ipojo.getInstanceByName(PUBLISHER_INSTANCE_NAME).stop();
		// stop test hub
		hub.stop();
		osgi.dispose();
		ipojo.dispose();

	}

	@Configuration
	public static Option[] configure() {
		Option[] platform = options(felix());

		Option[] bundles = options(provision(
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.ipojo")
						.versionAsInProject(),
				mavenBundle().groupId("org.osgi")
						.artifactId("org.osgi.compendium").versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-api")
						.versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple")
						.versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.testing")
						.artifactId("osgi-helpers").versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.json")
						.artifactId("json-service-json.org")
						.versionAsInProject(),
				mavenBundle().groupId("org.jabsorb")
						.artifactId("org.ow2.chameleon.commons.jabsorb")
						.versionAsInProject(),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.eventadmin")
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
				mavenBundle().groupId("org.ow2.chameleon.rose.jsonrpc")
						.artifactId("jabsorb-exporter").versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.rss")
						.artifactId("pubsubhubbub-publisher")
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
	 * Check publisher instance status
	 */
	@Test
	public void testActivity() {
		// wait for the service to be available.
		waitForIt(100);
		Assert.assertEquals(ComponentInstance.VALID,
				ipojo.getInstanceByName(PUBLISHER_INSTANCE_NAME).getState());

		waitForIt(100);

		// check RSS topic
		Assert.assertNull(reader.getLastEntry());

	}

	/**
	 * Checks publish request to hub
	 */
	@Test
	public void testRegistrationParameters() {

		Map<String, Object> parameters;

		// check POST parameters
		parameters = hub.getReqParams();
		for (String parameter : parameters.keySet()) {
			if (parameter.equals("Content-Type")) {
				Assert.assertTrue(parameters.get("Content-Type").equals(
						"application/x-www-form-urlencoded"));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE))[0]
						.equals("publish"));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL))[0]
						.equals("http://192.168.197.1:8080/roserss/"));
			}

		}

	}

	/**
	 * Check POST parameters send to hub in update action when new endpoint
	 * added
	 */
	@Test
	public void testUpdatePropertiesAddEndpoint() {

		EndpointListener endpLis;
		Map<String, Object> parameters;

		// perform endpointAdded in publisher
		endpLis = (EndpointListener) osgi.getServiceObject(
				EndpointListener.class.getName(), null);
		endpLis.endpointAdded(endp, null);

		waitForIt(100);

		// check POST parameters

		parameters = hub.getReqParams();
		for (String parameter : parameters.keySet()) {
			if (parameter.equals("Content-Type")) {
				Assert.assertTrue(parameters.get("Content-Type").equals(
						"application/x-www-form-urlencoded"));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE))[0]
						.equals("update"));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL))[0]
						.equals("http://192.168.197.1:8080/roserss/"));
			}
		}

	}

	/**
	 * Check RSS Feed when new endpoint added
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testRSSFeedAddEndpoint() {

		EndpointListener endpLis;
		EndpointDescription feedEndp;
		JSONService json;
		FeedEntry feed;

		// perform endpointAdded in publisher
		endpLis = (EndpointListener) osgi.getServiceObject(
				EndpointListener.class.getName(), null);
		endpLis.endpointAdded(endp, null);

		json = (JSONService) osgi.getServiceObject(JSONService.class.getName(),
				null);

		feed = reader.getLastEntry();
		Assert.assertTrue(feed.title().equals(RoseRSSConstants.FEED_TITLE_NEW));

		try {
			// get endpoint description feed RSS)
			feedEndp = RoseEndpointDescription.getEndpointDescription(json
					.fromJSON(feed.content()));
			// check discovered endpoint and published in RSS
			Assert.assertTrue(endp.equals(feedEndp));
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Check POST parameters send to hub in update action when new endpoint
	 * removed
	 */
	@Test
	public void testUpdatePropertiesRemoveEndpoint() {

		EndpointListener endpLis;
		Map<String, Object> parameters;

		// perform endpointAdded in publisher
		endpLis = (EndpointListener) osgi.getServiceObject(
				EndpointListener.class.getName(), null);
		endpLis.endpointAdded(endp, null);

		// remove endpoint
		endpLis.endpointRemoved(endp, null);

		waitForIt(100);

		// check POST parameters
		parameters = hub.getReqParams();
		for (String parameter : parameters.keySet()) {
			if (parameter.equals("Content-Type")) {
				Assert.assertTrue(parameters.get("Content-Type").equals(
						"application/x-www-form-urlencoded"));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE))[0]
						.equals("update"));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL))[0]
						.equals("http://192.168.197.1:8080/roserss/"));
			}
		}

	}

	/**
	 * Check RSS Feed when new endpoint removed
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testRSSFeedRemoveEndpoint() {

		FeedEntry feed;
		EndpointListener endpLis;
		EndpointDescription feedEndp;
		JSONService json;

		feed = reader.getLastEntry();

		// perform endpointAdded by publisher
		endpLis = (EndpointListener) osgi.getServiceObject(
				EndpointListener.class.getName(), null);
		endpLis.endpointAdded(endp, null);

		// remove endpoint
		endpLis.endpointRemoved(endp, null);

		json = (JSONService) osgi.getServiceObject(JSONService.class.getName(),
				null);

		feed = reader.getLastEntry();
		Assert.assertTrue(feed.title().equals(
				RoseRSSConstants.FEED_TITLE_REMOVE));

		try {
			// get endpoint description feed RSS
			feedEndp = RoseEndpointDescription.getEndpointDescription(json
					.fromJSON(feed.content()));
			// check discovered endpoint and published in RSS
			Assert.assertTrue(endp.equals(feedEndp));
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Check POST request parameters after stop publishing
	 */
	@Test
	public void testUnpublish() {
		// stop publisher.
		ipojo.getInstanceByName(PUBLISHER_INSTANCE_NAME).stop();

		Map<String, Object> parameters;

		// check POST parameters
		parameters = hub.getReqParams();
		for (String parameter : parameters.keySet()) {
			if (parameter.equals("Content-Type")) {
				Assert.assertTrue(parameters.get("Content-Type").equals(
						"application/x-www-form-urlencoded"));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_HUB_MODE))[0]
						.equals("unpublish"));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL))[0]
						.equals("http://192.168.197.1:8080/roserss/"));
			}

		}

		waitForIt(100);

		// check RSS topic
		Assert.assertNull(reader.getLastEntry());

	}

	public static void waitForIt(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			assert false;
		}
	}
}
