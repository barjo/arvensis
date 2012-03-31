package org.ow2.chameleon.pubsubhubbub.test.hub;

import static org.ow2.chameleon.rose.RoseMachine.ROSE_MACHINE_ID;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_RECONNECT;
import static org.ow2.chameleon.pubsubhubbub.test.clients.AbstractTestConfiguration.waitForIt;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub;
import org.ow2.chameleon.rose.pubsubhubbub.hub.Hub;
import org.ow2.chameleon.rose.pubsubhubbub.hub.Registrations;

/**
 * @author Bartek
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class DistributedHubTest extends AbstactHubTest {

	private static final String DISTRIBUTED_HUB_1_ALIAS = "/distributed1";
	private static final String DISTRIBUTED_HUB_1_INSTANCE_NAME = "distributed.hub.1";
	private static final String DISTRIBUTED_HUB_1_URI = "http://localhost:8080"
			+ DISTRIBUTED_HUB_1_ALIAS;

	private static final String DISTRIBUTED_HUB_2_ALIAS = "/distributed2";
	private static final String DISTRIBUTED_HUB_2_INSTANCE_NAME = "distributed.hub.2";
	private static final String DISTRIBUTED_HUB_2_URI = "http://localhost:8080"
			+ DISTRIBUTED_HUB_2_ALIAS;

	private static final String DISTRIBUTED_HUB_3_ALIAS = "/distributed3";
	private static final String DISTRIBUTED_HUB_3_INSTANCE_NAME = "distributed.hub.3";
	private static final String DISTRIBUTED_HUB_3_URI = "http://localhost:8080"
			+ DISTRIBUTED_HUB_3_ALIAS;

	private static final String TEST_PUBLISHER_MACHINEID_1 = "publisher1";
	private static final String TEST_PUBLISHER_ALIAS_1 = "/pub1";
	private static final String TEST_PUBLISHER_CALLBACK_1 = "http://localhost:8080"
			+ TEST_PUBLISHER_ALIAS_1;
	private static final String TEST_PUBLISHER_RSSURL_1 = "http://localhost:8080/pub1rss";

	private static final String TEST_PUBLISHER_MACHINEID_2 = "publisher2";
	private static final String TEST_PUBLISHER_ALIAS_2 = "/pub2";
	private static final String TEST_PUBLISHER_CALLBACK_2 = "http://localhost:8080"
			+ TEST_PUBLISHER_ALIAS_2;
	private static final String TEST_PUBLISHER_RSSURL_2 = "http://localhost:8080/pub2rss";

	private static final String TEST_SUBSCRIBER_MACHINEID_1 = "susbcriber1";
	private static final String TEST_SUBSCRIBER_ALIAS_1 = "/sub1";
	private static final String TEST_SUBSCRIBER_CALLBACK_1 = "http://localhost:8080"
			+ TEST_SUBSCRIBER_ALIAS_1;
	private static final String TEST_SUBSCRIBER_MACHINEID_2 = "subscriber2";
	private static final String TEST_SUBSCRIBER_ALIAS_2 = "/sub2";
	private static final String TEST_SUBSCRIBER_CALLBACK_2 = "http://localhost:8080"
			+ TEST_SUBSCRIBER_ALIAS_2;

	private static final String ROSE_1_MACHINE_ID = "roseHub1";
	private static final String ROSE_2_MACHINE_ID = "roseHub2";
	private static final String ROSE_3_MACHINE_ID = "roseHub3";

	private List<TestPubsubhubbub> pubsubhubbubs;

	private List<DistributedHub> distributedHubs;

	@Before
	@Override
	public final void setUp() throws UnknownHostException {
		super.setUp();

		pubsubhubbubs = new ArrayList<DistributedHubTest.TestPubsubhubbub>();

		distributedHubs = new ArrayList<DistributedHub>();

	}

	/**
	 * Check hub instance status.
	 */
	@Test
	public final void testActivity() {
		// create 1-st hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);

	}

	/**
	 * Check hub instances statuses. 2-nd connects to 1-st
	 */
	@Test
	public final void testTwoHubsActivity() {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);

	}

	/**
	 * Check hub instances statuses. 2-nd connects to 1-st, 3-rd connects to
	 * 2-nd. Plus check established connection between them
	 */
	@Test
	public final void testThreeHubsActivityAndConnection() {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 3-rd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_3_ALIAS,
				DISTRIBUTED_HUB_3_INSTANCE_NAME, DISTRIBUTED_HUB_2_URI,
				ROSE_3_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// check connections
		Assert.assertTrue(distributedHubs.get(0).getHubBackups()
				.getAllHubsLink().contains(DISTRIBUTED_HUB_2_URI));
		Assert.assertTrue(distributedHubs.get(1).getHubBackups()
				.getAllHubsLink().contains(DISTRIBUTED_HUB_1_URI));
		Assert.assertTrue(distributedHubs.get(1).getHubBackups()
				.getAllHubsLink().contains(DISTRIBUTED_HUB_3_URI));
		Assert.assertTrue(distributedHubs.get(2).getHubBackups()
				.getAllHubsLink().contains(DISTRIBUTED_HUB_2_URI));
	}

	/**
	 * Test unlink notification from 2-nd hub to 1-st
	 * 
	 * @throws BundleException
	 */
	@Test
	public final void testUnlink() throws BundleException {
		ComponentInstance hub2;

		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 2-nd hub, wait for the service to be available
		hub2 = startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// check if connected each other
		Assert.assertTrue(distributedHubs.get(0).getHubBackups()
				.getAllHubsLink().contains(DISTRIBUTED_HUB_2_URI));
		Assert.assertTrue(distributedHubs.get(1).getHubBackups()
				.getAllHubsLink().contains(DISTRIBUTED_HUB_1_URI));

		// stop 2-nd hub
		hub2.stop();
		waitForIt(WAIT_TIME);

		// no connection registered in 1-st hub
		Assert.assertFalse(distributedHubs.get(0).getHubBackups()
				.getAllHubsLink().contains(DISTRIBUTED_HUB_2_URI));
	}

	/**
	 * 3 hub connected at the beginning, on 1-st hub new endpoint is registered,
	 * check propagate
	 */
	@Test
	public final void testNewEndpoint() {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 3-rd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_3_ALIAS,
				DISTRIBUTED_HUB_3_INSTANCE_NAME, DISTRIBUTED_HUB_2_URI,
				ROSE_3_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// prepare endpoints
		createEndpoints();

		// register new endpoint in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addEndpointByMachineID(TEST_PUBLISHER_MACHINEID_1,
						testEndpoints.get(0));
		// send propagation from 1-st hub
		distributedHubs.get(0).addEndpoint(testEndpoints.get(0),
				TEST_PUBLISHER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// check if correctly propagate new endpoints
		Assert.assertTrue(pubsubhubbubs.get(0).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
		waitForIt(WAIT_TIME);
		Assert.assertTrue(pubsubhubbubs.get(1).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
		waitForIt(WAIT_TIME);
		Assert.assertTrue(pubsubhubbubs.get(2).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));

	}

	/**
	 * 1-st hub got at the beginning new endpoint, then 2-nd connects to 1-st,
	 * and 3-rd to 2-nd. Check getting new endpoints at linking stage
	 */
	@Test
	public final void testNewEndpoint2() {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// prepare endpoints
		createEndpoints();

		// register new endpoint in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addEndpointByMachineID(TEST_PUBLISHER_MACHINEID_1,
						testEndpoints.get(0));
		// send propagation from 1-st hub
		distributedHubs.get(0).addEndpoint(testEndpoints.get(0),
				TEST_PUBLISHER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 3-rd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_3_ALIAS,
				DISTRIBUTED_HUB_3_INSTANCE_NAME, DISTRIBUTED_HUB_2_URI,
				ROSE_3_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// check if correctly propagate new endpoint
		Assert.assertTrue(pubsubhubbubs.get(0).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
		Assert.assertTrue(pubsubhubbubs.get(1).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
		Assert.assertTrue(pubsubhubbubs.get(2).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
	}

	/**
	 * 1-st hub got at the beginning new endpoint, then 2-nd connects to 1-st,
	 * and 3-rd to 2-nd. After all 3-nd hub got new endpoint Check propagate
	 */
	@Test
	public final void testNewEndpoint3() {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// prepare endpoints
		createEndpoints();

		// register new endpoint in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addEndpointByMachineID(TEST_PUBLISHER_MACHINEID_1,
						testEndpoints.get(0));
		// send propagation from 1-st hub
		distributedHubs.get(0).addEndpoint(testEndpoints.get(0),
				TEST_PUBLISHER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 3-rd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_3_ALIAS,
				DISTRIBUTED_HUB_3_INSTANCE_NAME, DISTRIBUTED_HUB_2_URI,
				ROSE_3_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// register new endpoint in 3-rd pubsubhubub
		pubsubhubbubs
				.get(2)
				.getRegistrations()
				.addEndpointByMachineID(TEST_PUBLISHER_MACHINEID_2,
						testEndpoints.get(1));
		// send propagation from 3-rd hub
		distributedHubs.get(2).addEndpoint(testEndpoints.get(1),
				TEST_PUBLISHER_MACHINEID_2);
		waitForIt(WAIT_TIME);

		// check if correctly propagate new endpoints (0) from 1-st hub and (1)
		// from 3-rd hub
		Assert.assertTrue(pubsubhubbubs.get(0).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
		Assert.assertTrue(pubsubhubbubs.get(1).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
		Assert.assertTrue(pubsubhubbubs.get(2).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));

		waitForIt(WAIT_TIME);

		Assert.assertTrue(pubsubhubbubs.get(0).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(1)));
		Assert.assertTrue(pubsubhubbubs.get(1).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(1)));
		Assert.assertTrue(pubsubhubbubs.get(2).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(1)));
	}

	/**
	 * Testing an exchange registered endpoints on link stage 1-st hub has
	 * registered endpoint (0), 2-nd hub has registered endpoint (0). After all
	 * create connection between them
	 * 
	 * @throws ParseException
	 */
	@Test
	public final void testExchangeEndpoints() throws ParseException {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// prepare endpoints
		createEndpoints();

		// register new endpoint (0) in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addEndpointByMachineID(TEST_PUBLISHER_MACHINEID_1,
						testEndpoints.get(0));

		// send propagation from 1-st hub
		distributedHubs.get(0).addEndpoint(testEndpoints.get(0),
				TEST_PUBLISHER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, null, ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// register new endpoint (1) in 2-rd pubsubhubub
		pubsubhubbubs
				.get(1)
				.getRegistrations()
				.addEndpointByMachineID(TEST_PUBLISHER_MACHINEID_2,
						testEndpoints.get(1));
		// send propagation from 2-rd hub
		distributedHubs.get(1).addEndpoint(testEndpoints.get(1),
				TEST_PUBLISHER_MACHINEID_2);
		waitForIt(WAIT_TIME);

		// Check if hubs didn`t exchange their registered endpoints
		Assert.assertFalse(pubsubhubbubs.get(0).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(1)));
		Assert.assertFalse(pubsubhubbubs.get(1).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));

		// create an connection between hubs
		distributedHubs.get(0).establishConnection(DISTRIBUTED_HUB_2_URI);

		waitForIt(WAIT_TIME);

		// Check if hubs didn`t exchange their registered endpoints
		Assert.assertTrue(pubsubhubbubs.get(0).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(1)));
		Assert.assertTrue(pubsubhubbubs.get(1).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));

	}

	/**
	 * Testing an exchange registered subscribers on link stage 1-st hub has
	 * registered subscriber 1, 2-nd hub has registered subscriber 2. Create a
	 * connection between them and test their backup subscribers .After all
	 * remove subscriber 1
	 * 
	 * @throws ParseException
	 */
	@Test
	public final void testExchangeSubscribers() throws ParseException {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// register new subscriber 1 in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addSubscriber(TEST_SUBSCRIBER_CALLBACK_1, "",
						TEST_SUBSCRIBER_MACHINEID_1);

		waitForIt(WAIT_TIME);

		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, null, ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// register new subscriber 1 in 1-st pubsubhubub
		pubsubhubbubs
				.get(1)
				.getRegistrations()
				.addSubscriber(TEST_SUBSCRIBER_CALLBACK_2, "",
						TEST_SUBSCRIBER_MACHINEID_2);
		waitForIt(WAIT_TIME);

		// Check if hubs didn`t exchange their registered subscribers
		Assert.assertNull(distributedHubs.get(0).getHubBackups()
				.getSubscribers().get(ROSE_2_MACHINE_ID));
		Assert.assertNull(distributedHubs.get(1).getHubBackups()
				.getSubscribers().get(ROSE_1_MACHINE_ID));

		// create an connection between hubs
		distributedHubs.get(0).establishConnection(DISTRIBUTED_HUB_2_URI);

		waitForIt(WAIT_TIME);

		// check if successfully registered backup subscriber
		Assert.assertTrue(distributedHubs.get(0).getHubBackups()
				.getSubscribers().get(ROSE_2_MACHINE_ID)
				.contains(TEST_SUBSCRIBER_CALLBACK_2));
		Assert.assertTrue(distributedHubs.get(1).getHubBackups()
				.getSubscribers().get(ROSE_1_MACHINE_ID)
				.contains(TEST_SUBSCRIBER_CALLBACK_1));

		// remove subscriber from hub 1
		distributedHubs.get(0).removeBackupSubscriber(
				TEST_SUBSCRIBER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// check if successfully unregistered backup subscriber
		Assert.assertFalse(distributedHubs.get(1).getHubBackups()
				.getSubscribers().get(ROSE_1_MACHINE_ID)
				.contains(TEST_SUBSCRIBER_CALLBACK_1));
	}

	/**
	 * Testing an exchange registered subscribers. 3 hub connected, 1-st hub
	 * register subscriber, check if 3 hub didnt get any notification (only one
	 * step in backup)
	 * 
	 * @throws ParseException
	 */
	@Test
	public final void testExchangeSubscribers2() throws ParseException {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// create 3-rd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_3_ALIAS,
				DISTRIBUTED_HUB_3_INSTANCE_NAME, DISTRIBUTED_HUB_2_URI,
				ROSE_3_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// register new subscriber 1 in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addSubscriber(TEST_SUBSCRIBER_CALLBACK_1, "",
						TEST_SUBSCRIBER_MACHINEID_1);

		// notify other hub in this case will be only hub 2
		distributedHubs.get(0).addBackupSubscriber(TEST_SUBSCRIBER_MACHINEID_1,
				TEST_SUBSCRIBER_CALLBACK_1);

		waitForIt(WAIT_TIME);

		// check if successfully registered backup subscriber on hub 2
		Assert.assertTrue(distributedHubs.get(1).getHubBackups()
				.getSubscribers().get(ROSE_1_MACHINE_ID)
				.contains(TEST_SUBSCRIBER_CALLBACK_1));

		// check if hub3 hasnt got any registered subscribers
		Assert.assertNull(distributedHubs.get(2).getHubBackups()
				.getSubscribers().get(ROSE_1_MACHINE_ID));

	}

	/**
	 * Testing an exchange registered publishers on link stage 1-st hub has
	 * registered publisher 1, 2-nd hub has registered publisher 2. Create a
	 * connection between them and test their backup publisher .After all remove
	 * publisher 1
	 * 
	 * @throws ParseException
	 */
	@Test
	public final void testExchangePublishers() throws ParseException {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// register new publisher 1 in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addTopic(TEST_PUBLISHER_RSSURL_1, TEST_PUBLISHER_MACHINEID_1,
						TEST_PUBLISHER_CALLBACK_1);

		waitForIt(WAIT_TIME);

		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, null, ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// / register new publisher 1 in 1-st pubsubhubub
		pubsubhubbubs
				.get(1)
				.getRegistrations()
				.addTopic(TEST_PUBLISHER_RSSURL_2, TEST_PUBLISHER_MACHINEID_2,
						TEST_PUBLISHER_CALLBACK_2);
		waitForIt(WAIT_TIME);

		// Check if hubs didn`t exchange their registered subscribers
		Assert.assertNull(distributedHubs.get(0).getHubBackups()
				.getPublishers().get(ROSE_2_MACHINE_ID));
		Assert.assertNull(distributedHubs.get(1).getHubBackups()
				.getPublishers().get(ROSE_1_MACHINE_ID));

		// create an connection between hubs
		distributedHubs.get(0).establishConnection(DISTRIBUTED_HUB_2_URI);

		waitForIt(WAIT_TIME);

		// check if successfully registered backup publisher
		Assert.assertTrue(distributedHubs.get(0).getHubBackups()
				.getPublishers().get(ROSE_2_MACHINE_ID)
				.contains(TEST_PUBLISHER_CALLBACK_2));
		Assert.assertTrue(distributedHubs.get(1).getHubBackups()
				.getPublishers().get(ROSE_1_MACHINE_ID)
				.contains(TEST_PUBLISHER_CALLBACK_1));

		// remove publisher from hub 1
		distributedHubs.get(0)
				.removeBackupPublisher(TEST_PUBLISHER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// check if successfully unregistered backup subscriber
		Assert.assertFalse(distributedHubs.get(1).getHubBackups()
				.getPublishers().get(ROSE_1_MACHINE_ID)
				.contains(TEST_PUBLISHER_CALLBACK_2));
	}

	/**
	 * Testing an exchange registered publishers. 3 hub connected, 1-st hub
	 * register publisher, check if 3 hub didnt get any notification (only one
	 * step in backup)
	 * 
	 * @throws ParseException
	 */
	@Test
	public final void testExchangePublishers2() throws ParseException {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// create 3-rd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_3_ALIAS,
				DISTRIBUTED_HUB_3_INSTANCE_NAME, DISTRIBUTED_HUB_2_URI,
				ROSE_3_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// register new publisher 1 in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addTopic(TEST_PUBLISHER_RSSURL_1, TEST_PUBLISHER_MACHINEID_1,
						TEST_PUBLISHER_CALLBACK_1);

		// notify other hub in this case will be only hub 2
		distributedHubs.get(0).addBackupPublisher(TEST_PUBLISHER_MACHINEID_1,
				TEST_PUBLISHER_CALLBACK_1);

		waitForIt(WAIT_TIME);

		// check if successfully registered backup publisher on hub 2
		Assert.assertTrue(distributedHubs.get(1).getHubBackups()
				.getPublishers().get(ROSE_1_MACHINE_ID)
				.contains(TEST_PUBLISHER_CALLBACK_1));

		// check if hub3 hasnt got any registered subscribers
		Assert.assertNull(distributedHubs.get(2).getHubBackups()
				.getPublishers().get(ROSE_1_MACHINE_ID));

	}

	/**
	 * 3 hub connected at the beginning, after on 1-st hub, new endpoint is
	 * registered, and same endpoint is removed. Check propagate
	 */
	@Test
	public final void testRemoveEndpoint() {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 3-rd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_3_ALIAS,
				DISTRIBUTED_HUB_3_INSTANCE_NAME, DISTRIBUTED_HUB_2_URI,
				ROSE_3_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// prepare endpoints
		createEndpoints();

		// register new endpoint in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addEndpointByMachineID(TEST_PUBLISHER_MACHINEID_1,
						testEndpoints.get(0));
		// send propagation from 1-st hub
		distributedHubs.get(0).addEndpoint(testEndpoints.get(0),
				TEST_PUBLISHER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// remove endpoint in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.removeEndpoint(TEST_PUBLISHER_MACHINEID_1,
						testEndpoints.get(0).getServiceId());

		// send propagation from 1-st hub
		distributedHubs.get(0)
				.removeEndpoint(testEndpoints.get(0).getServiceId(),
						TEST_PUBLISHER_MACHINEID_1);

		waitForIt(WAIT_TIME);

		// check if correctly propagate remove endpoints
		Assert.assertFalse(pubsubhubbubs.get(0).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
		waitForIt(WAIT_TIME);
		Assert.assertFalse(pubsubhubbubs.get(1).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
		waitForIt(WAIT_TIME);
		Assert.assertFalse(pubsubhubbubs.get(2).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));

	}

	/**
	 * 3 hub connected at the beginning, after on 1-st hub, 2 new endpoints are
	 * registered, and one of them is removed. Check propagate
	 */
	@Test
	public final void testRemoveEndpoint2() {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);
		// create 3-rd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_3_ALIAS,
				DISTRIBUTED_HUB_3_INSTANCE_NAME, DISTRIBUTED_HUB_2_URI,
				ROSE_3_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// prepare endpoints
		createEndpoints();

		// register new endpoint(0) in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addEndpointByMachineID(TEST_PUBLISHER_MACHINEID_1,
						testEndpoints.get(0));
		// send propagation from 1-st hub
		distributedHubs.get(0).addEndpoint(testEndpoints.get(0),
				TEST_PUBLISHER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// register new endpoint(1) in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addEndpointByMachineID(TEST_PUBLISHER_MACHINEID_1,
						testEndpoints.get(1));
		// send propagation from 1-st hub
		distributedHubs.get(0).addEndpoint(testEndpoints.get(1),
				TEST_PUBLISHER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// remove endpoint in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.removeEndpoint(TEST_PUBLISHER_MACHINEID_1,
						testEndpoints.get(0).getServiceId());

		// send propagation from 1-st hub
		distributedHubs.get(0)
				.removeEndpoint(testEndpoints.get(0).getServiceId(),
						TEST_PUBLISHER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// check if correctly propagate remove endpoints
		Assert.assertFalse(pubsubhubbubs.get(0).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
		waitForIt(WAIT_TIME);
		Assert.assertFalse(pubsubhubbubs.get(1).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));
		waitForIt(WAIT_TIME);
		Assert.assertFalse(pubsubhubbubs.get(2).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(0)));

		// check if still register endpoint(1)
		Assert.assertTrue(pubsubhubbubs.get(0).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(1)));
		Assert.assertTrue(pubsubhubbubs.get(1).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(1)));
		Assert.assertTrue(pubsubhubbubs.get(2).getRegistrations()
				.getAllEndpoints().containsKey(testEndpoints.get(1)));

	}

	/**
	 * 2 hub connected at the beginning, subscriber and publisher is connected
	 * to 1-st hub which after a while is down. Test reconnect on 2-nd hub
	 */
	@Test
	public final void testReconnect() {
		// create 1-st hub, wait for the service to be available.
		startDistributedHub(DISTRIBUTED_HUB_1_ALIAS,
				DISTRIBUTED_HUB_1_INSTANCE_NAME, null, ROSE_1_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// prepare endpoints
		createEndpoints();

		// register new publisher 1 in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addTopic(TEST_PUBLISHER_RSSURL_1, TEST_PUBLISHER_MACHINEID_1,
						TEST_PUBLISHER_CALLBACK_1);
		waitForIt(WAIT_TIME);

		// create publisher 1
		TestPubsubhubbubClient publisher1 = new TestPubsubhubbubClient(
				TEST_PUBLISHER_ALIAS_1, TEST_PUBLISHER_RSSURL_1);

		// / register new subscriber 1 in 1-st pubsubhubub
		pubsubhubbubs
				.get(0)
				.getRegistrations()
				.addSubscriber(TEST_SUBSCRIBER_CALLBACK_1, "",
						TEST_SUBSCRIBER_MACHINEID_1);
		waitForIt(WAIT_TIME);

		// create publisher 1
		TestPubsubhubbubClient subscriber1 = new TestPubsubhubbubClient(
				TEST_SUBSCRIBER_ALIAS_1, "");

		// create 2-nd hub, wait for the service to be available
		startDistributedHub(DISTRIBUTED_HUB_2_ALIAS,
				DISTRIBUTED_HUB_2_INSTANCE_NAME, DISTRIBUTED_HUB_1_URI,
				ROSE_2_MACHINE_ID);
		waitForIt(WAIT_TIME);

		// imitate 1-st hub is down
		http.unregister(DISTRIBUTED_HUB_1_ALIAS);

		// register new endpoint(0) in 2-nd pubsubhubub
		pubsubhubbubs
				.get(1)
				.getRegistrations()
				.addEndpointByMachineID(TEST_PUBLISHER_MACHINEID_2,
						testEndpoints.get(1));

		// send propagation from 2-nd hub
		distributedHubs.get(1).addEndpoint(testEndpoints.get(1),
				TEST_PUBLISHER_MACHINEID_2);
		waitForIt(WAIT_TIME * 5);

		// check if correctly changed Pubsubhubbub url on publisher and
		// subscriber
		Assert.assertNotNull(publisher1.getUrlChanged());
		Assert.assertNotNull(subscriber1.getUrlChanged());

		// check if correctly register subscriber and publisher on 2-nd hub
		Assert.assertTrue(pubsubhubbubs.get(1).getRegistrations()
				.getPublishers().containsValue(TEST_PUBLISHER_MACHINEID_1));
		Assert.assertTrue(pubsubhubbubs.get(1).getRegistrations()
				.getSubscribers().containsValue(TEST_SUBSCRIBER_MACHINEID_1));

		waitForIt(WAIT_TIME * 10);

	}

	private ComponentInstance startDistributedHub(String alias,
			String instanceName, String bootstrapLink, String roseMachineId) {
		ServiceRegistration hubSreg;
		try {
			// run rose
			Dictionary<String, String> roseProps = new Hashtable<String, String>();
			roseProps.put(ROSE_MACHINE_ID, roseMachineId);

			ipojo.createComponentInstance("RoSe_machine", roseProps);

			ServiceReference roseSref = osgi.getServiceReference(
					RoseMachine.class.getName(), "(" + ROSE_MACHINE_ID + "="
							+ roseMachineId + ")");

			// run Test Pubsubhubbub
			TestPubsubhubbub hub = new TestPubsubhubbub();

			// store pubsubhubbub
			pubsubhubbubs.add(hub);

			// register TestPubsubhubbub
			hubSreg = context.registerService(Hub.class.getName(), hub, null);

			waitForIt(WAIT_TIME);

			Dictionary<String, Object> dHubProps = new Hashtable<String, Object>();

			// distributed hub instance properties
			dHubProps.put(DistributedHub.JERSEY_ALIAS_INSTANCE_PROPERTY, alias);
			dHubProps.put("instance.name", instanceName);

			// add "requires.filter" to distributed instance properties
			Dictionary<String, String> reqFi = new Hashtable<String, String>();
			reqFi.put("hubID", "(service.id="
					+ hubSreg.getReference().getProperty("service.id")
							.toString() + ")");
			reqFi.put("roseID",
					"(service.id="
							+ roseSref.getProperty("service.id").toString()
							+ ")");
			dHubProps.put("requires.filters", reqFi);

			// check if bootstrap.link property is set
			if (bootstrapLink != null) {
				dHubProps.put(DistributedHub.BOOTSTRAP_LINK_INSTANCE_PROPERTY,
						bootstrapLink);
			}

			// create a distributed hub instance
			ComponentInstance distributedInstance = ipojo.getFactory(
					DistributedHub.COMPONENT_NAME).createComponentInstance(
					dHubProps);

			// check instance state
			Assert.assertEquals(ComponentInstance.VALID,
					distributedInstance.getState());

			// retrieve instance metadata
			Element el = distributedInstance.getInstanceDescription()
					.getDescription();

			// check if connected to specified created before TestHub

			boolean pubsF = false;
			boolean roseF = false;
			for (Element rootElement : el.getElements("handler")) {

				// check only injection handler
				if (rootElement.getAttribute("name").equals(
						"org.apache.felix.ipojo:requires")) {

					// list all injections
					for (Element element : rootElement.getElements("requires")) {

						// check hub injection
						if (element.getAttribute("id").equals("hubID")) {

							// check all "uses"
							for (Element innerElement : element
									.getElements("uses")) {

								// check if uses particular testPubsubhubbub,
								// created before
								if (innerElement.getAttribute("service.id")
										.equals(hubSreg.getReference()
												.getProperty("service.id")
												.toString())) {

									pubsF = true;
								} else {
									Assert.fail("Wrong dependency of TestPubsubhubbub in DistributedHub  ("
											+ instanceName
											+ "). Uses "
											+ innerElement
													.getAttribute("service.id")
											+ " instead of "
											+ hubSreg.getReference()
													.getProperty("service.id")
													.toString());
								}
							}
						}

						// check rose injection
						if (element.getAttribute("id").equals("roseID")) {

							// check all "uses"
							for (Element innerElement : element
									.getElements("uses")) {

								// check if uses particular testPubsubhubbub,
								// created before
								if (innerElement.getAttribute("service.id")
										.equals(roseSref.getProperty(
												"service.id").toString())) {

									roseF = true;
								} else {
									Assert.fail("Wrong dependency of Rose in DistributedHub  ("
											+ instanceName
											+ "). Uses "
											+ innerElement
													.getAttribute("service.id")
											+ " instead of "
											+ roseSref
													.getProperty("service.id")
													.toString());
								}
							}
						}

					}
				}
			}
			if (!pubsF || !roseF) {
				Assert.fail("Dependency of TestPubsubhubbub or Rose in DistributedHub can`t be found");
			}
			// get distributedhub object
			distributedHubs.add((DistributedHub) osgi.getServiceObject(
					DistributedHub.class.getName(), "(instance.name="
							+ instanceName + ")"));
			return distributedInstance;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	private class TestPubsubhubbub implements Hub {
		private Registrations registrations;

		public TestPubsubhubbub() {
			super();
			this.registrations = new TestRegistrations();
		}

		public void start() {
		}

		public void stop() {
		}

		public Registrations getRegistrations() {
			return registrations;
		}

		@SuppressWarnings("unchecked")
		public EndpointDescription getEndpointDescriptionFromJSON(
				Map<String, Object> map) {

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

		public String getUrl() {
			return "localhost:8080/testPubs";
		}

	}

	private class TestRegistrations implements Registrations {

		Map<EndpointDescription, String> endpoints;
		Map<String, String> subscribers;
		Map<String, String> publishers;

		public TestRegistrations() {
			super();
			endpoints = new HashMap<EndpointDescription, String>();
			subscribers = new HashMap<String, String>();
			publishers = new HashMap<String, String>();
		}

		public void addEndpointByTopicRssUrl(String rssUrl,
				EndpointDescription endp) {
		}

		public boolean addEndpointByMachineID(String machineID,
				EndpointDescription endp) {
			if (endpoints.containsKey(endp)) {
				return false;
			}
			endpoints.put(endp, machineID);
			return true;
		}

		public void removeEndpointByTopicRssUrl(String rssUrl,
				EndpointDescription endp) {

		}

		public void removeSubscriber(String callBackUrl) {

		}

		public Map<EndpointDescription, String> getAllEndpoints() {
			return endpoints;
		}

		public boolean removeEndpoint(String machineID, long endpointId) {
			for (Entry<EndpointDescription, String> element : endpoints
					.entrySet()) {
				if (element.getKey().getServiceId() == (endpointId)) {
					endpoints.remove(element.getKey());
					return true;
				}
			}
			return false;
		}

		public String getPublisherMachineIdByRssUrl(String publisher) {
			return null;
		}

		public void addSubscriber(String callBackUrl, String endpointFilter,
				String machineID) {
			subscribers.put(callBackUrl, machineID);

		}

		public Map<String, String> getSubscribers() {
			return subscribers;
		}

		public Map<String, String> getPublishers() {
			return publishers;
		}

		public void addTopic(String rssURL, String machineID, String callbackUrl) {
			publishers.put(callbackUrl, machineID);
		}

	}

	/**
	 * Imitate a subscriber or publisher
	 * 
	 * @author Bartek
	 * 
	 */
	private class TestPubsubhubbubClient extends HttpServlet {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String returnPost;
		private String alias;
		private String urlChanged = null;

		public TestPubsubhubbubClient(String pAlias, String pReturnPost) {
			returnPost = pReturnPost;
			alias = pAlias;
			try {
				http.registerServlet(alias, this, null, null);
			} catch (Exception e) {
				Assert.fail("Error in registration a servlet");
				e.printStackTrace();
			}

		}

		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			if (req.getParameter(HTTP_POST_PARAMETER_RECONNECT) != null) {
				urlChanged = req.getParameter(HTTP_POST_PARAMETER_RECONNECT);
				resp.getWriter().append(returnPost);
				resp.setStatus(HttpStatus.SC_OK);

			} else {
				resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			}
		}

		public String getUrlChanged() {
			return urlChanged;
		}

	}

}
