package org.ow2.chameleon.rose.jsonrpc;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ow2.chameleon.rose.introspect.EndpointCreatorIntrospection.ENDPOINT_CONFIG_PREFIX;

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
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.testing.EndpointCreatorAbstractTest;

/**
 * Integration test for the jabsorb-endpoint-creator component.
 * @author barjo
 */
@RunWith(JUnit4TestRunner.class)
public class EndpointCreatorTest extends EndpointCreatorAbstractTest {
    private static final String FILTER="("+ENDPOINT_CONFIG_PREFIX+"=jsonrpc)";
    private static URI JSONRPC_URI;

    @Before
    public void setUp() {
        super.setUp();
        
        //init the url
        try {
			JSONRPC_URI = new URI("http://localhost:" + HTTP_PORT + "/JSONRPC");
		} catch (URISyntaxException e) {
		}
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
                mavenBundle().groupId("org.ow2.chameleon.rose.jsonrpc").artifactId("jabsorb-endpoint-creator").versionAsInProject()
		));
	}

    @SuppressWarnings("unchecked")
	protected <T> T getProxy(ExportRegistration xreg,Class<T> itface) {
    	T proxy = null;
        Session session = new HTTPSession(JSONRPC_URI);
        Client client = new Client(session);
        proxy = (T) client.openProxy(xreg.getExportReference().getExportedEndpoint().getId(), itface);
    	
		return proxy;
	}

    protected ExporterService getExporterService(){
    	return rose.getServiceObject(ExporterService.class, FILTER);
    }
}

