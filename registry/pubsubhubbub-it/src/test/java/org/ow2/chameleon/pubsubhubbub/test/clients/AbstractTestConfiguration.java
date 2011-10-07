package org.ow2.chameleon.pubsubhubbub.test.clients;

import static org.mockito.MockitoAnnotations.initMocks;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

/**
 * Abstract class for publisher and subscriber test, contains some common
 * setups.
 * 
 * @author Bartek
 * 
 */
public class AbstractTestConfiguration {

	@Inject
	private BundleContext context;

	private HttpService http;

	private OSGiHelper osgi;

	private IPOJOHelper ipojo;

	private TestHubImpl hub;

	private EndpointDescription endp;

	private JSONService json;

	@Before
	public void setUp() throws UnknownHostException {

		osgi = new OSGiHelper(context);
		ipojo = new IPOJOHelper(context);

		initMocks(this);

		// get http service
		http = (HttpService) osgi.getServiceObject(HttpService.class.getName(),
				null);

		// get json service
		json = (JSONService) osgi.getServiceObject(JSONService.class.getName(),
				null);

		// run a test hub, first response status accepted for registration a
		// tested publisher
		hub = new TestHubImpl(http, HttpStatus.SC_CREATED);
		hub.start();

		// prepare test endpoint description
		Map<String, Object> endpProps = new HashMap<String, Object>();
		endpProps.put(OBJECTCLASS, new String[] { "testObject" });
		endpProps.put(ENDPOINT_ID, "1");
		endpProps.put(SERVICE_IMPORTED_CONFIGS,
				new String[] { "import configs" });
		endp = new EndpointDescription(endpProps);

		// run rose
		ipojo.createComponentInstance("RoSe_machine");


	}

	/**
	 * Global bundle configuration.
	 * 
	 * @return global options for paxexam
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
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-api")
						.versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple")
						.versionAsInProject(),
				mavenBundle().groupId("org.jabsorb")
						.artifactId("org.ow2.chameleon.commons.jabsorb")
						.versionAsInProject(),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.eventadmin")
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
				mavenBundle().groupId("org.ow2.chameleon.rose")
						.artifactId("pubsubhubbub").versionAsInProject()

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

	public static void waitForIt(final int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			assert false;
		}
	}

	public final BundleContext getContext() {
		return context;
	}

	public final HttpService getHttp() {
		return http;
	}

	public final OSGiHelper getOsgi() {
		return osgi;
	}

	public final IPOJOHelper getIpojo() {
		return ipojo;
	}

	public final TestHubImpl getHub() {
		return hub;
	}

	public final EndpointDescription getEndp() {
		return endp;
	}

	public final JSONService getJson() {
		return json;
	}
}
