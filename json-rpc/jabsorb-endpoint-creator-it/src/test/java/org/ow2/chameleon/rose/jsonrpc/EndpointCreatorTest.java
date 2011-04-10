package org.ow2.chameleon.rose.jsonrpc;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.ow2.chameleon.rose.introspect.EndpointCreatorIntrospection.ENDPOINT_CONFIG_PREFIX;
import static org.ow2.chameleon.rose.jsonrpc.ITTools.waitForIt;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.jabsorb.client.Client;
import org.jabsorb.client.HTTPSession;
import org.jabsorb.client.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.device.Device;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

/**
 * Integration test for the jabsorb-endpoint-creator component
 * @author barjo
 */
@RunWith(JUnit4TestRunner.class)
public class EndpointCreatorTest {

    private static final String FILTER="("+ENDPOINT_CONFIG_PREFIX+"=jsonrpc)";
    private static final String HTTP_PORT = "9027";
    private static URI JSONRPC_URI;

    /*
     * Number of mock object by test.
     */
    private static final int MAX_MOCK = 10;

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;
    
    private IPOJOHelper ipojo;
    
    @Mock private LogService logService; //Mock LogService
    @Mock private Device device; //Mock Device

    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
        
        //initialise the annoted mock object
        initMocks(this);
        
        //init the url
        try {
			JSONRPC_URI = new URI("http://localhost:" + HTTP_PORT + "/JSON-RPC");
		} catch (URISyntaxException e) {
		}
    }

    @After
    public void tearDown() {
        osgi.dispose();
        ipojo.dispose();
    }

    @Configuration
    public static Option[] configure() {
        Option[] platform = options(felix(),systemProperty( "org.osgi.service.http.port" ).value( HTTP_PORT ));

        Option[] bundles = options(provision(
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").versionAsInProject(),
                mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject(), 
                mavenBundle().groupId("org.ow2.chameleon.rose").artifactId("rose-core").versionAsInProject(), 
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").versionAsInProject(), 
                mavenBundle().groupId("org.slf4j").artifactId("slf4j-api").versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple").versionAsInProject(),
                mavenBundle().groupId("com.sun.grizzly.osgi").artifactId("grizzly-httpservice-bundle").versionAsInProject(), 
                mavenBundle().groupId("org.json").artifactId("org.ow2.chameleon.commons.json").versionAsInProject(),
                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-httpclient").versionAsInProject(),
                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-codec").versionAsInProject(),
                mavenBundle().groupId("commons-logging").artifactId("org.ow2.chameleon.commons.logging").versionAsInProject(),
                mavenBundle().groupId("org.jabsorb").artifactId("org.ow2.chameleon.commons.jabsorb").versionAsInProject(),
                mavenBundle().groupId("org.ow2.chameleon.rose.jsonrpc").artifactId("jabsorb-endpoint-creator").versionAsInProject()
                )); 

        Option[] r = OptionUtils.combine(platform, bundles);

        return r;
    }

    /**
     * Mockito bundles
     * @return
     */
    @Configuration
    public static Option[] mockitoBundle() {
        return options(JUnitOptions.mockitoBundles());
    }

    /**
     * Basic Test, in order to know if the {@link ExporterService} service is correctly provided.
     */
    @Test
    public void testAvailability() {
    	//wait for the service to be available.
        waitForIt(100);
        
        ExporterService exporter = getExporterService(); //Get the ExporterService 
        
        assertNotNull(exporter); //Check that the exporter != null
    }

    /**
     * Test the {@link ExporterService#exportService(ServiceReference, Map)} with 
     * a valid {@link ServiceReference}.
     */
    @Test
    public void testExportService() {
        //wait for the service to be available.
        waitForIt(100);
        
        ExporterService exporter = getExporterService(); //get the service
        
        //Register a mock LogService
        ServiceRegistration regLog = registerService(logService);
        
        //export the logService 
        ExportRegistration xreg = exporter.exportService(regLog.getReference(), null);
        
        //check that xreg is not null
        assertNotNull(xreg); 
        
        //check that there is no exception
        assertNull(xreg.getException());
        
        //check that the export reference is not null
        assertNotNull(xreg.getExportReference());
        
        //check that the ServiceReference is equal to the logService one
        assertEquals(regLog.getReference(), xreg.getExportReference().getExportedService());
        
        //get a proxy
        LogService proxy = getProxy(xreg,LogService.class);
        
        //check proxy != null
        assertNotNull(proxy);
        
        //check proxy calls
        for (int i = 1; i <= MAX_MOCK; i++) {
            proxy.log(LOG_WARNING, "YEAHH!!"+i);
            verify(logService).log(LOG_WARNING, "YEAHH!!"+i);
        }
        
        //check that there is no side effects calling the service
        Mockito.verifyNoMoreInteractions(logService);
    }
    
    @SuppressWarnings("unchecked")
	private <T> T getProxy(ExportRegistration xreg,Class<T> itface) {
    	T proxy = null;
            Session session = new HTTPSession(JSONRPC_URI);
            Client client = new Client(session);
            proxy = (T) client.openProxy(xreg.getExportReference().getExportedEndpoint().getId(), itface);
    	
		return proxy;
	}

	private ExporterService getExporterService(){
    	return (ExporterService) osgi.getServiceObject(ExporterService.class.getName(), FILTER);
    }
    
    private <T> ServiceRegistration registerService(T service){
    	return context.registerService(((T) service).getClass().getName(), service, null);
    }
}

