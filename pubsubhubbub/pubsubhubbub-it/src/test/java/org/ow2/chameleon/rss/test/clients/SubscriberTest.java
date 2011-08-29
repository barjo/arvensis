package org.ow2.chameleon.rss.test.clients;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.constants.RoseRSSConstants;

@RunWith(JUnit4TestRunner.class)
public class SubscriberTest extends AbstractTestConfiguration{

	private static final String SUBSCRIBER_INSTANCE_NAME = "Rose_Pubsubhubbub.subscriber-1";
	private static final String ENDPOINT_FILTER = "(endpoint.id=*)";
	private static final String CALLBACK_URL = "/sub1";
	private static final String HUB_URL = "http://localhost:8080/hub";

	private String publisherCallBackUrl;

	private RoseMachine rose;


	@Before
	@Override
	public void setUp() throws UnknownHostException {
		super.setUp();
		// run RoSe machine
		ipojo.createComponentInstance("RoSe_machine");
		initMocks(this);

		// create subscriber instance, register subscriber in hub
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("hub.url", HUB_URL);
		props.put("callback.url", CALLBACK_URL);
		props.put("endpoint.filter", ENDPOINT_FILTER);
		props.put("instance.name", SUBSCRIBER_INSTANCE_NAME);
		ipojo.createComponentInstance("Rose_Pubsubhubbub.subscriber", props);

		// change hub response status
		hub.changeResponseStatus(HttpStatus.SC_ACCEPTED);

		// get Rose
		rose = (RoseMachine) osgi.getServiceObject(RoseMachine.class.getName(),
				null);

		// create subscriber call back url
		publisherCallBackUrl = "http://"
				+ InetAddress.getLocalHost().getHostAddress() + ":8080"
				+ CALLBACK_URL;

	}

	@After
	public void tearDown() {

		// response to unregister subscriber
		System.out.println("ending");
		hub.changeResponseStatus(HttpStatus.SC_ACCEPTED);

		// stop subscriber, wait for response from test hub
		ipojo.getInstanceByName(SUBSCRIBER_INSTANCE_NAME).stop();

		// stop test hub
		hub.stop();
		osgi.dispose();
		ipojo.dispose();

	}

	@Configuration
	public static Option[] configure() {
		Option[] platform = options(felix());

		Option[] bundles = options(provision(
				mavenBundle().groupId("org.ow2.chameleon.rss")
						.artifactId("pubsubhubbub-subscriber")
						.versionAsInProject()
		));

		Option[] r = OptionUtils.combine(platform, bundles);

		return r;
	}

	/**
	 * Check subscriber instance status
	 */
	@Test
	public void testActivity() {
		// wait for the service to be available.
		waitForIt(100);
		Assert.assertEquals(ComponentInstance.VALID,
				ipojo.getInstanceByName(SUBSCRIBER_INSTANCE_NAME).getState());
		// check remote registration at beginning
		Assert.assertFalse(rose.containsRemote(endp));

	}

	/**
	 * Checks publish request to hub
	 * 
	 * @throws UnknownHostException
	 */
	@Test
	public void testSubscriptionParameters() {

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
						.equals("subscribe"));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_ENDPOINT_FILTER)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_ENDPOINT_FILTER))[0]
						.equals(ENDPOINT_FILTER));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_URL_CALLBACK)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_URL_CALLBACK))[0]
						.equals(publisherCallBackUrl));
			}

		}

	}

	/**
	 * Hub update notification send to subscriber - Endpoint added
	 */
	@Test
	public void testEndpointAddedNotification() {

		hub.sendUpdate(RoseRSSConstants.HUB_UPDATE_ENDPOINT_ADDED,
				publisherCallBackUrl, endp, json);
		waitForIt(100);

		// check if endpoint successfully registered
		Assert.assertTrue(rose.containsRemote(endp));
	}

	/**
	 * Hub update notification send to subscriber - Endpoint removed
	 */
	@Test
	public void testEndpointRemoveNotification() {

		hub.sendUpdate(RoseRSSConstants.HUB_UPDATE_ENDPOINT_ADDED,
				publisherCallBackUrl, endp, json);
		waitForIt(100);
		hub.sendUpdate(RoseRSSConstants.HUB_UPDATE_ENDPOINT_REMOVED,
				publisherCallBackUrl, endp, json);
		// check if endpoint successfully unregistered
		Assert.assertFalse(rose.containsRemote(endp));
	}

	/**
	 * Check POST request parameters after stop subscribing
	 */

	/**
	 * Check POST request parameters after stop subscribing
	 */
	@Test
	public void testUnpublish() {
		// stop susbcriber
		ipojo.getInstanceByName(SUBSCRIBER_INSTANCE_NAME).stop();

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
						.equals("unsubscribe"));
			} else if (parameter
					.equals(RoseRSSConstants.HTTP_POST_PARAMETER_URL_CALLBACK)) {
				Assert.assertTrue(((String[]) parameters
						.get(RoseRSSConstants.HTTP_POST_PARAMETER_URL_CALLBACK))[0]
						.equals(publisherCallBackUrl));
			}

		}

	}

}