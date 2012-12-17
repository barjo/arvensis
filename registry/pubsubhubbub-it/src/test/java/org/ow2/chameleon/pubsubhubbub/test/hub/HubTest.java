package org.ow2.chameleon.pubsubhubbub.test.hub;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.ow2.chameleon.pubsubhubbub.test.clients.AbstractTestConfiguration.waitForIt;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.FEED_TITLE_NEW;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.FEED_TITLE_REMOVE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ow2.chameleon.rose.pubsubhubbub.hub.Hub;

@RunWith(JUnit4TestRunner.class)
public class HubTest extends AbstactHubTest {

	@Before
	@Override
	public final void setUp() throws UnknownHostException {
		super.setUp();
		// create a hub instance
		
		ipojo.createComponentInstance("RoSe_machine");
		
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put(Hub.INSTANCE_PROPERTY_HUB_URL, HUB_RELATIVE_PATH);
		props.put("instance.name", HUB_INSTANCE_NAME);
		ipojo.createComponentInstance(Hub.COMPONENT_NAME, props);

		hubUrl = "http://localhost:8080" + HUB_RELATIVE_PATH;

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
		publisher = new TestPublisher("/rss", "subscriber1", "/call1");
		publisher.registerPublisher();
		// hub can not register the publisher, because of rss topic not found
		Assert.assertEquals(SC_BAD_REQUEST, hubResponseCode);
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
		publisher = new TestPublisher("/rss", "subscriber1", "/call1");
		publisher.createRSSTopic();
		publisher.registerPublisher();

		// hub successfully register a publisher
		Assert.assertEquals(SC_CREATED, hubResponseCode);
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
		publisher = new TestPublisher("/rss", "subscriber1", "/call1");
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
		publisher = new TestPublisher("/rss", "subscriber1", "/call1");

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
		publisher = new TestPublisher("/rss", "subscriber1", "/call1");

		// create RSS topic
		publisher.createRSSTopic();

		// prepare endpoints;
		createEndpoints();

		// send publish notification to Hub
		publisher.registerPublisher();

		// endpoint add
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_NEW);
		waitForIt(WAIT_TIME);
		publisher.sendUpdateToHub();
		waitForIt(WAIT_TIME);

		// endpoint remove
		publisher.addRSSFeed(testEndpoints.get(0), FEED_TITLE_REMOVE);
		waitForIt(WAIT_TIME);
		publisher.sendUpdateToHub();
		waitForIt(WAIT_TIME);
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
		subscriber = new TestSubscriber("/sub1", "()", "sub1");

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
		publisher = new TestPublisher("/rss", "subscriber1", "/call1");
		;

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
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=*)", "sub1");

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
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=*)", "sub1");

		// start subscriber
		subscriber.start();
		waitForIt(WAIT_TIME);

		// create publisher and send publish notification to Hub
		publisher = new TestPublisher("/rss", "subscriber1", "/call1");

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
		publisher = new TestPublisher("/rss", "subscriber1", "/call1");

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
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=0)", "sub1");

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
		publisher = new TestPublisher("/rss", "subscriber1", "/call1");

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
				"(|(endpoint.id=0)(endpoint.id=2))", "sub1");

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
		publisher = new TestPublisher("/rss", "subscriber1", "/call1");

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
		publisher2 = new TestPublisher("/rss2", "subscriber2", "/call2");

		// create RSS topic
		publisher2.createRSSTopic();

		// send publish notification to Hub (register endpoint id=2)
		publisher2.registerPublisher();
		publisher2.addRSSFeed(testEndpoints.get(2), FEED_TITLE_NEW);

		waitForIt(WAIT_TIME);

		// send update to hub
		publisher2.sendUpdateToHub();

		// create subscriber
		subscriber = new TestSubscriber("/sub1", "(endpoint.id=*)", "sub1");

		// start subscriber
		subscriber.start();
		waitForIt(WAIT_TIME);

		// publisher2 stops
		publisher.stop();
		waitForIt(WAIT_TIME * 5);
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

		waitForIt(WAIT_TIME * 5);
		// check expected update and got ones
		Assert.assertTrue(subscriber.checkUpdates(expectUpdates));
		waitForIt(WAIT_TIME);
		// stop subscriber and publisher
		subscriber.stop();
		publisher2.stop();

	}

}
