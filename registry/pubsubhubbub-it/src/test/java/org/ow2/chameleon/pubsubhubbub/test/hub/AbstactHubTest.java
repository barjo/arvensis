package org.ow2.chameleon.pubsubhubbub.test.hub;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_SERVICE_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.ow2.chameleon.pubsubhubbub.test.clients.AbstractTestConfiguration.waitForIt;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_HEADER_TYPE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_ENDPOINT_FILTER;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_HUB_MODE;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_MACHINEID;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_RSS_TOPIC_URL;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_URL_CALLBACK;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_UPDATE_CONTENT;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION;
import static org.ow2.chameleon.syndication.FeedReader.FEED_TITLE_PROPERTY;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.container.def.options.VMOption;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseEndpointDescription;
import org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HubMode;
import org.ow2.chameleon.syndication.FeedEntry;
import org.ow2.chameleon.syndication.FeedWriter;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

public abstract class AbstactHubTest {
	
	protected static final String HUB_RELATIVE_PATH = "/hub";

	protected static final String HUB_INSTANCE_NAME = "Rose_Pubsubhubbub.hub";

	protected static final int WAIT_TIME = 100;

	@Inject
	protected BundleContext context;

	protected HttpService http;

	protected OSGiHelper osgi;

	protected IPOJOHelper ipojo;

	protected String hubUrl;

	protected DefaultHttpClient client;

	protected int hubResponseCode;

	protected JSONService json;

	protected List<EndpointDescription> testEndpoints;
	
	
	public void setUp() throws UnknownHostException {

		osgi = new OSGiHelper(context);
		ipojo = new IPOJOHelper(context);

		initMocks(this);

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
				mavenBundle().groupId("org.ow2.chameleon.rose.registry")
						.artifactId("pubsubhubbub").versionAsInProject(),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.eventadmin")
						.versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey")
						.artifactId("jersey-client").versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey")
						.artifactId("jersey-server").versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey")
						.artifactId("jersey-core").versionAsInProject()

		));
		Option[] r = OptionUtils.combine(platform, bundles);
		//for debugging purpose
		Option[] r2  = OptionUtils.combine(r, new VMOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5006"));
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
	 * Subscriber for testing purpose.
	 * 
	 * @author Bartek
	 * 
	 */
	final class TestSubscriber extends HttpServlet {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String subscriberRelativeUrl;

		private String filter;
		private String subscriberFullUrl;
		private String machineId;
		private List<EndpointTitle> postParameters;

		TestSubscriber(final String pSubscriberRelativeUrl,
				final String pFilter, final String pMachineID) {
			this.subscriberRelativeUrl = pSubscriberRelativeUrl;
			this.filter = pFilter;
			this.machineId=pMachineID;
			this.postParameters = new ArrayList<EndpointTitle>();
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
				nvps.add(new BasicNameValuePair(
						HTTP_POST_PARAMETER_MACHINEID, this.machineId));

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
			nvps.add(new BasicNameValuePair(
					HTTP_POST_PARAMETER_MACHINEID, this.machineId));

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
			} catch (Exception e) {
				System.err.println(req.getParameter(HTTP_POST_UPDATE_CONTENT));
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
		boolean checkUpdate(final EndpointTitle expected) {

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
		boolean checkUpdates(final List<EndpointTitle> expectUpdates) {
			// check if expected and received are the same, order doesn`t count
			if ((new HashSet<EndpointTitle>(postParameters))
					.equals(new HashSet<EndpointTitle>(expectUpdates))) {
				return true;
			}
			return false;
		}
	}

	 final class TestPublisher {

		private String fullRssUrl;
		private String relativeRssUrl;
		private FeedWriter writer;
		private int feedIndex = 0;
		private String machineID;
		private String callBackUrl;

		public TestPublisher(final String pRelativeRssUrl, String machineID, String callBackUrl) {
			this.relativeRssUrl = pRelativeRssUrl;

			// creating full url address to rss topic
			this.fullRssUrl = "http://localhost:8080" + pRelativeRssUrl;
			this.machineID = machineID;
			this.callBackUrl="http://localhost:8080" +callBackUrl;
		}

		public void addRSSFeed(final EndpointDescription endpointDescription,
				final String feedTitleNew) {
			FeedEntry feed;
			feedIndex++;
			feed = writer.createFeedEntry();
			feed.title(feedTitleNew);
			feed.content("1" + feedIndex
					+ json.toJSON(endpointDescription.getProperties()));
			try {
				writer.addEntry(feed);
			} catch (Exception e) {
				e.printStackTrace();
				Assert.fail("Feed not send");
			}

		}

		protected void createRSSTopic() {
			// create RSS topic
			Dictionary<String, String> rssServletProps = new Hashtable<String, String>();
			rssServletProps.put(FEED_TITLE_PROPERTY, "RoseRss");
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

		void registerPublisher() {
			// preparing POST parameters
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
					HubMode.publish.toString()));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_MACHINEID,
					machineID));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_RSS_TOPIC_URL,
					this.fullRssUrl));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_URL_CALLBACK,
					this.callBackUrl));

			sendPOST(nvps);
		}

		void stop() {
			// preparing POST parameters
			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_HUB_MODE,
					HubMode.unpublish.toString()));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_RSS_TOPIC_URL,
					this.fullRssUrl));
			nvps.add(new BasicNameValuePair(HTTP_POST_PARAMETER_MACHINEID,
					machineID));
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
		static class EndpointTitle {
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
		
		/**
		 * Fill testEndpoints with simple @EndpointDescription.
		 */
		void createEndpoints() {
			Map<String, Object> endpProps = new HashMap<String, Object>();
			EndpointDescription endp;
			int index;
			final int endpointsNb = 3;

			testEndpoints = new ArrayList<EndpointDescription>();
			for (index = 0; index < endpointsNb; index++) {
				endpProps.put(OBJECTCLASS, new String[] { "testObject" });
				endpProps.put(ENDPOINT_ID, String.valueOf(index));
				endpProps.put(ENDPOINT_SERVICE_ID, new Long(index));
				endpProps.put(SERVICE_IMPORTED_CONFIGS,
						new String[] { "import configs" });
				endp = new EndpointDescription(endpProps);
				testEndpoints.add(index, endp);
			}
		}


}
