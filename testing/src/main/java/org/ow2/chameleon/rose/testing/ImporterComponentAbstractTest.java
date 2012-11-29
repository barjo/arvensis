package org.ow2.chameleon.rose.testing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import java.util.Map;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.ow2.chameleon.rose.testing.RoSeHelper.waitForIt;

public abstract class ImporterComponentAbstractTest {
	
	protected static String HTTP_PORT = "8042";
	
    /*
     * Number of mock object by test.
     */
	protected static final int MAX_MOCK = 10;

    @Inject
    protected BundleContext context;

    protected OSGiHelper osgi;
    
    protected IPOJOHelper ipojo;
    
    protected RoSeHelper rose;
    
	@Mock private LogService logService; //Mock LogService
    @Mock private LogEntry logEntry; //Mock Device

    /**
     * Done some initializations.
     */
    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
        rose = new RoSeHelper(context);
        
        //initialise the annoted mock object
        initMocks(this);
    }

    /**
     * Closing the test.
     */
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
                mavenBundle().groupId("org.ow2.chameleon.rose.testing").artifactId("rose-helpers").versionAsInProject(),
                mavenBundle().groupId("org.ow2.chameleon.rose").artifactId("rose-core").versionAsInProject(), 
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").versionAsInProject(), 
                mavenBundle().groupId("org.slf4j").artifactId("slf4j-api").versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple").versionAsInProject()
                )); 

        Option[] r = OptionUtils.combine(platform, bundles);

        return r;
    }
    
    /**
     * Mockito bundles
     */
    @Configuration
    public static Option[] mockitoBundle() {
        return options(JUnitOptions.mockitoBundles());
    }
    
	
    /**
     * Basic Test, in order to know if the {@link ImporterService} service is correctly provided.
     */
    @Test
    public void testAvailability() {
    	//wait for the service to be available.
        waitForIt(100);
        
        ImporterService importer = getImporterService(); //Get the ExporterService 
        
        assertNotNull(importer); //Check that the exporter != null
    }

    /**
     * Test the {@link ImporterService#importService(EndpointDescription, Map)} with 
     * a valid {@link EndpointDescription}.
     */
    @Test
    public void testImportService() {
        //wait for the service to be available.
        waitForIt(100);
        
        ImporterService importer = getImporterService(); //get the service
        
        //create an endpoint for logService
        EndpointDescription desc = createEndpoint("toto", LogService.class,logService);
        
        //import the logService 
        ImportRegistration ireg = importer.importService(desc, null);
        
        //check that xreg is not null
        assertNotNull(ireg); 
        
        //check that there is no exception
        assertNull(ireg.getException());
        
        //check that the import reference is not null
        assertNotNull(ireg.getImportReference());
        
        //check that the EndpointDescription is equal to the endpoint one
        assertEquals(ireg.getImportReference().getImportedEndpoint(), desc);
        
        //get the client
        LogService proxy = (LogService) context.getService(ireg.getImportReference().getImportedService());
        
        //check that the client is not null
        assertNotNull(proxy);
        
        //check proxy calls
        for (int i = 1; i <= MAX_MOCK; i++) {
            proxy.log(LOG_WARNING, "YEAHH!!"+i);
            verify(logService).log(LOG_WARNING, "YEAHH!!"+i);
       }
    }
    
    /**
     * Test the {@link ImporterService#importService(EndpointDescription, Map)} with 
     * a valid {@link EndpointDescription}.
     */
    @Test
    public void testImportServiceNoVoid() {
        //wait for the service to be available.
        waitForIt(100);
        
        ImporterService importer = getImporterService(); //get the service
        
        //create an endpoint for logService
        EndpointDescription desc = createEndpoint("toto", LogEntry.class,logEntry);
        
        //import the logService 
        ImportRegistration ireg = importer.importService(desc, null);
        
        //check that xreg is not null
        assertNotNull(ireg); 
        
        //check that there is no exception
        assertNull(ireg.getException());
        
        //check that the import reference is not null
        assertNotNull(ireg.getImportReference());
        
        //check that the EndpointDescription is equal to the endpoint one
        assertEquals(ireg.getImportReference().getImportedEndpoint(), desc);
        
        //get the client
        LogEntry proxy = (LogEntry) context.getService(ireg.getImportReference().getImportedService());
        
        //check that the client is not null
        assertNotNull(proxy);
        
        //check proxy calls
        for (int i = 1; i <= MAX_MOCK; i++) {
        	when(logEntry.getMessage()).thenReturn("toto"+i);
            String msg = proxy.getMessage();
            assertEquals("toto"+i, msg);
            verify(logEntry,times(i)).getMessage();
        }
        
        verifyNoMoreInteractions(logEntry);
    }
    
    /**
     * Test the  {@link ImporterService#importService(EndpointDescription, Map)} with 
     * a valid {@link ServiceReference}. import, destroy and re import.
     */
    @Test
    public void testReImportService() {
        //wait for the service to be available.
        waitForIt(100);
        
        ImporterService importer = getImporterService(); //get the service
        
        //create an endpoint for logService
        EndpointDescription desc = createEndpoint("toto", LogService.class,logService);
        
        //import the logService 
        ImportRegistration ireg = importer.importService(desc, null);
        
        //Close the import
        ireg.close();
        
        //re import
        ireg = importer.importService(desc, null);
        
        //check that xreg is not null
        assertNotNull(ireg); 
        
        //check that there is no exception
        assertNull(ireg.getException());
        
        //check that the import reference is not null
        assertNotNull(ireg.getImportReference());
        
        //check that the EndpointDescription is equal to the endpoint one
        assertEquals(ireg.getImportReference().getImportedEndpoint(), desc);
        
        //get the client
        LogService proxy = (LogService) context.getService(ireg.getImportReference().getImportedService());
        
        //check that the client is not null
        assertNotNull(proxy);
        
        //check proxy calls
        for (int i = 1; i <= MAX_MOCK; i++) {
            proxy.log(LOG_WARNING, "YEAHH!!"+i);
            verify(logService).log(LOG_WARNING, "YEAHH!!"+i);
        }
        
        verifyNoMoreInteractions(logService);
    }
  

	/**
     * Test the {@link ImportRegistration#close()}. (destroy the proxy)
     */
    @Test
    public void testCloseExportRegistration() {
        //wait for the service to be available.
        waitForIt(100);
        
        ImporterService importer = getImporterService(); //get the service
        
        //create an endpoint for logService
        EndpointDescription desc = createEndpoint("toto", LogService.class,logService);
        
        //import the logService 
        ImportRegistration ireg = importer.importService(desc, null);
        
        //backup the ImportReference & ServiceReference
        ImportReference iref = ireg.getImportReference();
        ServiceReference sref = iref.getImportedService();
        
        //Close the import
        ireg.close();
        
        //Check that the ImportRegistration has been successfully closed
        assertNull(ireg.getImportReference());
        assertNull(ireg.getException());
        
        //Check that the ImportReference has been succesfully destroyed
        assertFalse(importer.getAllImportReference().contains(iref));
        
        //Check that the proxy is no more available on the local registry
        assertNull(context.getService(sref));
    }
    
    /**
     * @return The {@link ImporterService} to be tested.
     */
    protected abstract ImporterService getImporterService();
    
    /**
     */
    protected abstract <T> EndpointDescription createEndpoint(String endpointId, Class<T> klass, T object);
}
