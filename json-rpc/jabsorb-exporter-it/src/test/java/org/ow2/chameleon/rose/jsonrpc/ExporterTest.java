package org.ow2.chameleon.rose.jsonrpc;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ow2.chameleon.rose.ExporterService.ENDPOINT_CONFIG_PREFIX;

import java.net.URI;
import java.net.URISyntaxException;

import org.jabsorb.client.Client;
import org.jabsorb.client.HTTPSession;
import org.jabsorb.client.Session;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.testing.ExporterComponentAbstractTest;

/**
 * Integration test for the jabsorb-endpoint-creator component.
 * @author barjo
 */
@RunWith(JUnit4TestRunner.class)
public class ExporterTest extends ExporterComponentAbstractTest {
    private static final String FILTER="("+ENDPOINT_CONFIG_PREFIX+"=jsonrpc)";
    private static final String PROP_JABSORB_URL="org.jabsorb.url";

    @Before
    public void setUp() {
        super.setUp();
    }
    
    
    @Configuration
	public static Option[] endpointCreatorBundle() {
		return CoreOptions.options(CoreOptions.provision(
                mavenBundle().groupId("com.sun.grizzly.osgi").artifactId("grizzly-httpservice-bundle").versionAsInProject(),
				mavenBundle().groupId("org.json").artifactId("org.ow2.chameleon.commons.json").versionAsInProject(),
                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-httpclient").versionAsInProject(),
                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-codec").versionAsInProject(),
                mavenBundle().groupId("commons-logging").artifactId("org.ow2.chameleon.commons.logging").versionAsInProject(),
                mavenBundle().groupId("org.jabsorb").artifactId("org.ow2.chameleon.commons.jabsorb").versionAsInProject(),
                mavenBundle().groupId("org.ow2.chameleon.rose.jsonrpc").artifactId("jabsorb-exporter").versionAsInProject()
		));
	}

    @SuppressWarnings("unchecked")
	protected <T> T getProxy(ExportRegistration xreg,Class<T> itface) {
    	T proxy = null;
    	Session session = null;
    	
    	EndpointDescription description = xreg.getExportReference().getExportedEndpoint();

    	try {
			session = new HTTPSession(new URI((String) description.getProperties().get(PROP_JABSORB_URL)));
		} catch (URISyntaxException e) {
			fail("Bad Url computation"+e.getMessage());
		}
		
        Client client = new Client(session);
        proxy = (T) client.openProxy(description.getId(), itface);
    	
		return proxy;
	}

    protected ExporterService getExporterService(){
    	return rose.getServiceObject(ExporterService.class, FILTER);
    }
}

