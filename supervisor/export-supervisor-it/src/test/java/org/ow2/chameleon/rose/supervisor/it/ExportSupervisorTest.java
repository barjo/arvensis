package org.ow2.chameleon.rose.supervisor.it;

import static org.apache.felix.ipojo.ComponentInstance.INVALID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

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
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.JUnitOptions;
import org.osgi.framework.BundleContext;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

@RunWith(JUnit4TestRunner.class)
public class ExportSupervisorTest {
    private static final String EXPORT_SUPERVISOR_FACTORY = "rose.export-supervisor";

    /*
     * Number of mock object by test.
     */
    private static final int NB_MOCK = 10;

    @Inject
    private BundleContext context;

    private OSGiHelper osgi;
    
    private IPOJOHelper ipojo;

    @Before
    public void setUp() {
        osgi = new OSGiHelper(context);
        ipojo = new IPOJOHelper(context);
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
    	Dictionary<String, String> properties = new Hashtable<String, String>();
		properties.put("export.filter", "(export.service=true)");
		ComponentInstance instance = null;
		try {
			instance = ipojo.createComponentInstance(
					EXPORT_SUPERVISOR_FACTORY, properties);
		} catch (Exception e) {
			fail("Unable to create an export-supervisor instance, "+e.getMessage());
		}
    	
		//The instance must be invalid since there is no ExporterService
		assertEquals(INVALID, instance.getState()); 
    }
    

}
