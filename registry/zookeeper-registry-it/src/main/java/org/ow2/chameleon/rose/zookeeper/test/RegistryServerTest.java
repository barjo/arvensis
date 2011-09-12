package org.ow2.chameleon.rose.zookeeper.test;

import static org.apache.zookeeper.CreateMode.EPHEMERAL;
import static org.apache.zookeeper.CreateMode.PERSISTENT;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.service.log.LogService.LOG_DEBUG;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.ow2.chameleon.rose.ImporterService.ENDPOINT_CONFIG_PREFIX;
import static org.ow2.chameleon.rose.zookeeper.ZookeeperManager.computePath;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.Factory;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;


@RunWith(JUnit4TestRunner.class)
public class RegistryServerTest {
	private static final String PROP_JABSORB_URL = "org.jabsorb.url";
	protected static final String SERVLETNAME = "/JSONRPC";
	private static final String HTTP_PORT = "2170";
	private static final String ZOOKEEPER_HOST = "127.0.0.1";
	
	private RoseMachine rose;

	@Inject
	protected BundleContext context;

	protected OSGiHelper osgi;

	protected IPOJOHelper ipojo;

	private ZooKeeper keeper;
	
	private JSONService json;

	@Before
	public void setUp() {
		osgi = new OSGiHelper(context);
		ipojo = new IPOJOHelper(context);
		ipojo.createComponentInstance("RoSe_machine");
		Dictionary<String, String> props = new Hashtable<String, String>();
		props.put("connection", "127.0.0.1/rose");
		ipojo.createComponentInstance("RoSe_registry.zookeeper", props);
		rose = (RoseMachine) osgi.getServiceObject(RoseMachine.class.getName(), null);
		json = (JSONService) osgi.getServiceObject(JSONService.class.getName(),null);


		
	}

	@After
	public void tearDown() {
		osgi.dispose();
		ipojo.dispose();
	}

	/**
	 * Mockito bundles
	 */
	@Configuration
	public static Option[] mockitoBundle() {
		return options(JUnitOptions.mockitoBundles());
	}

	@Configuration
	public static Option[] configure() {
		Option[] platform = options(felix());

		Option[] bundles = options(provision(
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.ipojo")
						.versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.testing")
						.artifactId("osgi-helpers").versionAsInProject(),
				mavenBundle().groupId("org.osgi")
						.artifactId("org.osgi.compendium").versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-api")
						.versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple")
						.versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.rose")
						.artifactId("rose-core").versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.rose.registry")
						.artifactId("zookeeper-registry").versionAsInProject(),
				mavenBundle().groupId("org.jabsorb")
						.artifactId("org.ow2.chameleon.commons.jabsorb")
						.versionAsInProject(),
				mavenBundle().groupId("org.apache.felix")
						.artifactId("org.apache.felix.http.jetty")
						.versionAsInProject(),
				mavenBundle().groupId("org.apache.httpcomponents")
						.artifactId("httpcore-osgi").versionAsInProject(),
				mavenBundle().groupId("org.apache.httpcomponents")
						.artifactId("httpclient-osgi").versionAsInProject(),
				mavenBundle().groupId("commons-logging")
						.artifactId("org.ow2.chameleon.commons.logging")
						.versionAsInProject(),
				mavenBundle()
						.groupId("org.apache.cxf.dosgi")
						.artifactId(
								"cxf-dosgi-ri-discovery-distributed-zookeeper-wrapper")
						.versionAsInProject(),
				mavenBundle().groupId("org.apache.log4j")
						.artifactId("com.springsource.org.apache.log4j")
						.versionAsInProject(),
				mavenBundle().groupId("org.ow2.chameleon.json")
						.artifactId("json-service-json.org").versionAsInProject()

		));

		Option[] r = OptionUtils.combine(platform, bundles);

		return r;
	}

	protected <T> EndpointDescription createEndpoint(String endpointId,
			Class<T> klass) {

		Map<String, Object> props = new HashMap<String, Object>();
		props.put(PROP_JABSORB_URL, "http://localhost:" + HTTP_PORT
				+ SERVLETNAME);
		props.put(ENDPOINT_ID, endpointId);
		props.put(OBJECTCLASS, new String[] { klass.getName() });
		props.put(SERVICE_IMPORTED_CONFIGS, "jsonrpc");
		EndpointDescription desc = new EndpointDescription(props);
		return desc;
	}

	@Test
	public void test() throws InvalidSyntaxException, InterruptedException {
		connectToZookeeper();
		waitForIt(200);
		createNode("server1");
		EndpointDescription desc = createEndpoint("dummyEndpoint1",
				this.getClass());
		putToZookeeper("server1","endpoint1", desc);
		waitForIt(500);
		assertTrue((rose.containsRemote(desc)));
		waitForIt(200);
		clearZookeeper();
		waitForIt(200);
	}

	private void createNode(String node) {
		byte[] data = {0};
		try {
			keeper.create("/rose/"+node, data,ZooDefs.Ids.OPEN_ACL_UNSAFE, PERSISTENT);
			for (String b : keeper.getChildren("/rose", false)) {
				System.err.println(b);
			}
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	private void clearZookeeper() {
		try {
			for (String node : keeper.getChildren("/rose",null)) {
				for (String endpoint : keeper.getChildren("/rose/"+node, false)) {
					keeper.delete("/rose/"+node+"/"+endpoint, -1);
				}	
				keeper.delete("/rose/"+node, -1);
			}
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
	}

	private void putToZookeeper(String node, String endpoint, EndpointDescription endpointDesc) {
		String desc = json.toJSON(endpointDesc.getProperties());
		try {
			keeper.create("/rose/"+node+"/"+endpoint, desc.getBytes(),
					ZooDefs.Ids.OPEN_ACL_UNSAFE, PERSISTENT);
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void connectToZookeeper() {
		try {
			keeper = new ZooKeeper(ZOOKEEPER_HOST, 40000, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    public static void waitForIt(int time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            assert false;
        }
    }

}
