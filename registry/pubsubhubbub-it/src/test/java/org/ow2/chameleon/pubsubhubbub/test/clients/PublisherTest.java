package org.ow2.chameleon.pubsubhubbub.test.clients;

import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.FEED_TITLE_NEW;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.FEED_TITLE_REMOVE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_HUB_MODE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseEndpointDescription;
import org.ow2.chameleon.rose.pubsubhubbub.publisher.Publisher;
import org.ow2.chameleon.syndication.FeedEntry;
import org.ow2.chameleon.syndication.FeedReader;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

/**
 * Publisher test.
 * 
 * @author Bartek
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class PublisherTest extends AbstractTestConfiguration {

	private static final String PUBLISHER_INSTANCE_NAME = "Rose_Pubsubhubbub.publisher-1";

	private static final int WAIT_TIME = 100;

	private FeedReader reader;

	private String publisherRssUrl;

	private OSGiHelper osgi;

	private IPOJOHelper ipojo;

	private TestHubImpl hub;

	private EndpointDescription endp;

	private JSONService json;

	@Override
	public void setUp() throws UnknownHostException {
		
		super.setUp();
		
		osgi = super.getOsgi();
		ipojo = super.getIpojo();
		hub = super.getHub();
		endp = super.getEndp();
		json = super.getJson();

		// create publisher instance, register publisher in hub
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(Publisher.INSTANCE_PROPERTY_HUB_URL,
				"http://localhost:8080/hub");
		props.put(Publisher.INSTANCE_PROPERTY_RSS_URL, "/roserss");
		props.put("instance.name", PUBLISHER_INSTANCE_NAME);
		ipojo.createComponentInstance("Rose_Pubsubhubbub.publisher", props);

		// change hub response status
		hub.changeResponseStatus(HttpStatus.SC_ACCEPTED);

		// prepare rss feed reader, created by publisher
		reader = (FeedReader) osgi.getServiceObject(FeedReader.class.getName(),
				null);

		// create publisher RSS URL
		publisherRssUrl = "http://localhost:8080/roserss/";

	}

	@After
	public final void tearDown() {

		// response to unregister publisher

		hub.changeResponseStatus(HttpStatus.SC_ACCEPTED);

		// stop publisher, waits for response from test hub
		ipojo.getInstanceByName(PUBLISHER_INSTANCE_NAME).stop();

		// stop test hub
		hub.stop();
		osgi.dispose();
		ipojo.dispose();

	}

	/**
	 * Mockito bundles.
	 * 
	 * @return option contains mockito
	 */
	@Configuration
	public static Option[] mockitoBundle() {
		return options(JUnitOptions.mockitoBundles());
	}

	/**
	 * Check publisher instance status.
	 */
	@Test
	public final void testActivity() {
		// wait for the service to be available.
		waitForIt(WAIT_TIME);
		Assert.assertEquals(ComponentInstance.VALID,
				ipojo.getInstanceByName(PUBLISHER_INSTANCE_NAME).getState());

		waitForIt(WAIT_TIME);

		// check RSS topic
		Assert.assertNull(reader.getLastEntry());

	}

	/**
	 * Checks publish request to hub.
	 */
	@Test
	public final void testRegistrationParameters() {

		Map<String, Object> parameters;

		// check POST parameters
		parameters = hub.getReqParams();
		for (String parameter : parameters.keySet()) {
			if (parameter.equals("Content-Type")) {
				Assert.assertTrue(parameters.get("Content-Type").equals(
						"application/x-www-form-urlencoded"));
			} else if (parameter.equals(HTTP_POST_PARAMETER_HUB_MODE)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_HUB_MODE))[0]
						.equals("publish"));
			} else if (parameter.equals(HTTP_POST_PARAMETER_RSS_TOPIC_URL)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_RSS_TOPIC_URL))[0]
						.equals(publisherRssUrl));
			}

		}

	}

	/**
	 * Check POST parameters send to hub in update action when new endpoint
	 * added.
	 */
	@Test
	public final void testUpdatePropertiesAddEndpoint() {

		EndpointListener endpLis;
		Map<String, Object> parameters;

		// perform endpointAdded in publisher
		endpLis = (EndpointListener) osgi.getServiceObject(
				EndpointListener.class.getName(), null);
		endpLis.endpointAdded(endp, null);

		waitForIt(WAIT_TIME);

		// check POST parameters

		parameters = hub.getReqParams();
		for (String parameter : parameters.keySet()) {
			if (parameter.equals("Content-Type")) {
				Assert.assertTrue(parameters.get("Content-Type").equals(
						"application/x-www-form-urlencoded"));
			} else if (parameter.equals(HTTP_POST_PARAMETER_HUB_MODE)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_HUB_MODE))[0].equals("update"));
			} else if (parameter.equals(HTTP_POST_PARAMETER_RSS_TOPIC_URL)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_RSS_TOPIC_URL))[0]
						.equals(publisherRssUrl));
			}
		}

	}

	/**
	 * Check RSS Feed when new endpoint added.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public final void testRSSFeedAddEndpoint() {

		EndpointListener endpLis;
		EndpointDescription feedEndp;

		FeedEntry feed;

		// perform endpointAdded in publisher
		endpLis = (EndpointListener) osgi.getServiceObject(
				EndpointListener.class.getName(), null);
		endpLis.endpointAdded(endp, null);

		feed = reader.getLastEntry();
		Assert.assertTrue(feed.title().equals(FEED_TITLE_NEW));

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
	 * removed.
	 */
	@Test
	public final void testUpdatePropertiesRemoveEndpoint() {

		EndpointListener endpLis;
		Map<String, Object> parameters;

		// perform endpointAdded in publisher
		endpLis = (EndpointListener) osgi.getServiceObject(
				EndpointListener.class.getName(), null);
		endpLis.endpointAdded(endp, null);

		// remove endpoint
		endpLis.endpointRemoved(endp, null);

		waitForIt(WAIT_TIME);

		// check POST parameters
		parameters = hub.getReqParams();
		for (String parameter : parameters.keySet()) {
			if (parameter.equals("Content-Type")) {
				Assert.assertTrue(parameters.get("Content-Type").equals(
						"application/x-www-form-urlencoded"));
			} else if (parameter.equals(HTTP_POST_PARAMETER_HUB_MODE)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_HUB_MODE))[0].equals("update"));
			} else if (parameter.equals(HTTP_POST_PARAMETER_RSS_TOPIC_URL)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_RSS_TOPIC_URL))[0]
						.equals(publisherRssUrl));
			}
		}

	}

	/**
	 * Check RSS Feed when new endpoint removed.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public final void testRSSFeedRemoveEndpoint() {

		FeedEntry feed;
		EndpointListener endpLis;
		EndpointDescription feedEndp;

		feed = reader.getLastEntry();

		// perform endpointAdded by publisher
		endpLis = (EndpointListener) osgi.getServiceObject(
				EndpointListener.class.getName(), null);
		endpLis.endpointAdded(endp, null);

		// remove endpoint
		endpLis.endpointRemoved(endp, null);

		feed = reader.getLastEntry();
		Assert.assertTrue(feed.title().equals(FEED_TITLE_REMOVE));

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
	 * Check POST request parameters after stop publishing.
	 */
	@Test
	public final void testUnpublish() {
		// stop publisher.
		ipojo.getInstanceByName(PUBLISHER_INSTANCE_NAME).stop();

		Map<String, Object> parameters;

		// check POST parameters
		parameters = hub.getReqParams();
		for (String parameter : parameters.keySet()) {
			if (parameter.equals("Content-Type")) {
				Assert.assertTrue(parameters.get("Content-Type").equals(
						"application/x-www-form-urlencoded"));
			} else if (parameter.equals(HTTP_POST_PARAMETER_HUB_MODE)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_HUB_MODE))[0]
						.equals("unpublish"));
			} else if (parameter.equals(HTTP_POST_PARAMETER_RSS_TOPIC_URL)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_RSS_TOPIC_URL))[0]
						.equals(publisherRssUrl));
			}

		}

		waitForIt(WAIT_TIME);

		// check RSS topic
		Assert.assertNull(reader.getLastEntry());

	}
}
