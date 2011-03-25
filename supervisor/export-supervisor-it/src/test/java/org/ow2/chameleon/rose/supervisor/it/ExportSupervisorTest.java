package org.ow2.chameleon.rose.supervisor.it;

import static org.apache.felix.ipojo.ComponentInstance.INVALID;
import static org.apache.felix.ipojo.ComponentInstance.VALID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ow2.chameleon.rose.supervisor.it.ITTools.waitForIt;

import java.awt.HeadlessException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.ipojo.ComponentInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith(JUnit4TestRunner.class)
public class ExportSupervisorTest {
    private static final String EXPORT_SUPERVISOR_FACTORY = "rose.export-supervisor";

    /*
     * Number of mock object by test.
     */
    private static final int NB_MOCK = 10;

    //Properties used to track the service to be exported
    private static final String EXPORT_PROPERTY = "export.service";

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;
    
    private IPOJOHelper ipojo;
    
    @Mock(answer=RETURNS_MOCKS) private ExporterService exporter; //Mock ExporterService
    @Mock private ExportRegistration expreg;

    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
        
        //initialise the annoted mock object
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        osgi.dispose();
    }

    @Configuration
    public static Option[] configure() {
        Option[] platform = options(felix());

        Option[] bundles = options(provision(
        		mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").versionAsInProject(),
                mavenBundle().groupId("org.ow2.chameleon.testing").artifactId("osgi-helpers").versionAsInProject(), 
                mavenBundle().groupId("org.ow2.chameleon.rose").artifactId("rose-core").versionAsInProject(), 
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").versionAsInProject(),
                mavenBundle().groupId("org.slf4j").artifactId("slf4j-api").versionAsInProject(),
                mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple").versionAsInProject(),
        		// The target
                mavenBundle().groupId("org.ow2.chameleon.rose.supervisor").artifactId("export-supervisor").versionAsInProject())); 

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
     * Configure some test bundles
     * @return
     */
/*    @Configuration
    public static Option[] mockBundles() {

        return options(provision(newBundle().set(Constants.BUNDLE_SYMBOLICNAME, REMOTE_SERVICE_TRACKER_BUNDLE).set(Constants.BUNDLE_NAME,
                REMOTE_SERVICE_TRACKER_BUNDLE).add(RemoteServiceTracker.class).build(withBnd())));
    }*/

    /**
     * Test if the factory is valid and able to create instances.
     */
    @Test
    public void testInstanceCreation() {
    	ComponentInstance instance = createInstance();
    	
		//The instance must be invalid since there is no ExporterService
		assertEquals(INVALID, instance.getState()); 
		
		//register an exporter service
		ServiceRegistration reg = registerExporterService();
		
		waitForIt(200);
		
		//The instance must be valid since there is an ExporterService ;)
		assertEquals(VALID, instance.getState()); 
		
		//Goodbye ExporterService
		reg.unregister();
    }

    @Test 
    public void testServiceExport(){
    	createInstance();
    	registerExporterService();
    	
    	//register a mock lock service which must be exported
    	ServiceRegistration reg = createAndRegisterServiceToBeExported(LogService.class);
    	
    	waitForIt(200);
    	
    	//Check is the exported has been successfully called
    	verify(exporter).exportService(reg.getReference(), null);
    }
    
    private ComponentInstance createInstance(){
    	Dictionary<String, String> properties = new Hashtable<String, String>();
		properties.put("export.filter", "("+EXPORT_PROPERTY+"=true)");
		ComponentInstance instance = null;
		try {
			instance = ipojo.createComponentInstance(
					EXPORT_SUPERVISOR_FACTORY, properties);
		} catch (Exception e) {
			fail("Unable to create an export-supervisor instance, "+e.getMessage());
		}
		
		return instance;
    }
    
    private ServiceRegistration registerExporterService(){
    	return context.registerService(ExporterService.class.getName(), exporter, null);
    }
    
    private ServiceRegistration createAndRegisterServiceToBeExported(Class<?> clazz){
    	Dictionary<String, Object> properties = new Hashtable<String, Object>();
    	properties.put(EXPORT_PROPERTY, true);
    	Object service = mock(clazz);
    	return context.registerService(clazz.getName(), service , properties);
    }
}

