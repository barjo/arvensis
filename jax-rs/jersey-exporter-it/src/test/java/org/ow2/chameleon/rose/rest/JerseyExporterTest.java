package org.ow2.chameleon.rose.rest;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ow2.chameleon.rose.ExporterService.ENDPOINT_CONFIG_PREFIX;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.testing.ExporterComponentAbstractTest;

/**
 * Integration test for the jersey-exporter component.
 * @author barjo
 */
@RunWith(JUnit4TestRunner.class)
public class JerseyExporterTest extends ExporterComponentAbstractTest {
    private static final String FILTER="("+ENDPOINT_CONFIG_PREFIX+"=rest)";

    @Before
    public void setUp() {
        super.setUp();
        ipojo.createComponentInstance("RoSe_machine");
    	ipojo.createComponentInstance("RoSe_exporter.jersey");
    }
    
    
    @Configuration
	public static Option[] endpointCreatorBundle() {
		return CoreOptions.options(CoreOptions.provision(
                mavenBundle().groupId("com.sun.grizzly.osgi").artifactId("grizzly-httpservice-bundle").versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-server").versionAsInProject(),
                mavenBundle().groupId("com.sun.jersey").artifactId("jersey-core").versionAsInProject(),
                mavenBundle().groupId("javax.mail").artifactId("mail").versionAsInProject(),
                mavenBundle().groupId("javax.xml.bind").artifactId("jaxb-api-osgi").versionAsInProject(),
                mavenBundle().groupId("org.ow2.chameleon.rose.rest").artifactId("jersey-exporter").versionAsInProject()
		));
	}

    protected <T> T getProxy(ExportRegistration xreg,Class<T> itface) {
		return null;
	}

    protected ExporterService getExporterService(){
    	return rose.getServiceObject(ExporterService.class, FILTER);
    }
}

