package org.ow2.chameleon.pubsubhubbub.test.clients;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_ENDPOINT_FILTER;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_HUB_MODE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_URL_CALLBACK;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_MACHINEID;


import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

/**
 * Subscriber test.
 * 
 * @author Bartek
 * 
 */
@RunWith(JUnit4TestRunner.class)
public class SubscriberTest extends AbstractTestConfiguration {

	private static final String SUBSCRIBER_INSTANCE_NAME = "Rose_Pubsubhubbub.subscriber-1";
	private static final String ENDPOINT_FILTER = "(endpoint.id=*)";
	private static final String CALLBACK_URL = "/sub1";
	private static final String HUB_URL = "http://localhost:8080/hub";
	private static final int WAIT_TIME = 100;

	private String publisherCallBackUrl;

	private RoseMachine rose;

	private OSGiHelper osgi;

	private IPOJOHelper ipojo;

	private TestHubImpl hub;

	private EndpointDescription endp;

	private JSONService json;

	@Override
	public final  void setUp() throws UnknownHostException {
		
		super.setUp();
		
		osgi = super.getOsgi();
		ipojo = super.getIpojo();
		hub = super.getHub();
		endp = super.getEndp();
		json = super.getJson();

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
		publisherCallBackUrl = "http://localhost:8080" + CALLBACK_URL;

	}

	@After
	public final void tearDown() {

		// response to unregister subscriber
		hub.changeResponseStatus(HttpStatus.SC_ACCEPTED);

		// stop subscriber, wait for response from test hub
		ipojo.getInstanceByName(SUBSCRIBER_INSTANCE_NAME).stop();

		// stop test hub
		hub.stop();
		osgi.dispose();
		ipojo.dispose();

	}

	/**
	 * Check subscriber instance status.
	 */
	@Test
	public final void testActivity() {
		// wait for the service to be available.
		waitForIt(WAIT_TIME);
		Assert.assertEquals(ComponentInstance.VALID,
				ipojo.getInstanceByName(SUBSCRIBER_INSTANCE_NAME).getState());
		// check remote registration at beginning
		Assert.assertFalse(rose.containsRemote(endp));

	}

	/**
	 * Checks publish request to hub.
	 * 
	 * @throws UnknownHostException
	 */
	@Test
	public final void testSubscriptionParameters() {

		Map<String, Object> parameters;

		// check POST parameters
		parameters = hub.getReqParams();
		
		//check number of parameters
		Assert.assertTrue(parameters.size()==4);
		
		for (String parameter : parameters.keySet()) {
			if (parameter
					.equals(HTTP_POST_PARAMETER_HUB_MODE)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_HUB_MODE))[0]
						.equals("subscribe"));
			} else if (parameter
					.equals(HTTP_POST_PARAMETER_ENDPOINT_FILTER)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_ENDPOINT_FILTER))[0]
						.equals(ENDPOINT_FILTER));
			} else if (parameter
					.equals(HTTP_POST_PARAMETER_URL_CALLBACK)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_URL_CALLBACK))[0]
						.equals(publisherCallBackUrl));
			}
			else if (parameter
					.equals(HTTP_POST_PARAMETER_MACHINEID)) {
				Assert.assertNotNull(((String[]) parameters
						.get(HTTP_POST_PARAMETER_MACHINEID))[0]);
			}

		}

	}

	/**
	 * Hub update notification send to subscriber - Endpoint added.
	 */
	@Test
	public final void testEndpointAddedNotification() {
		waitForIt(WAIT_TIME);
		hub.sendUpdate(HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED,
				publisherCallBackUrl, endp, json);

		// check if endpoint successfully registered
		Assert.assertTrue(rose.containsRemote(endp));
	}

	/**
	 * Hub update notification send to subscriber - Endpoint removed.
	 */
	@Test
	public final void testEndpointRemoveNotification() {

		hub.sendUpdate(HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED,
				publisherCallBackUrl, endp, json);
		waitForIt(WAIT_TIME);
		hub.sendUpdate(
				HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED,
				publisherCallBackUrl, endp, json);
		// check if endpoint successfully unregistered
		Assert.assertFalse(rose.containsRemote(endp));
	}

	/**
	 * Check POST request parameters after stop subscribing.
	 */
	@Test
	public final void testUnsubscribe() {
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
					.equals(HTTP_POST_PARAMETER_HUB_MODE)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_HUB_MODE))[0]
						.equals("unsubscribe"));
			} else if (parameter
					.equals(HTTP_POST_PARAMETER_URL_CALLBACK)) {
				Assert.assertTrue(((String[]) parameters
						.get(HTTP_POST_PARAMETER_URL_CALLBACK))[0]
						.equals(publisherCallBackUrl));
			}
			else if (parameter
					.equals(HTTP_POST_PARAMETER_MACHINEID)) {
				Assert.assertNotNull(((String[]) parameters
						.get(HTTP_POST_PARAMETER_MACHINEID))[0]
						.equals(publisherCallBackUrl));
			}

		}

	}

}