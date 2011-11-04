package org.ow2.chameleon.rose.ws;

import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ow2.chameleon.rose.ExporterService.ENDPOINT_CONFIG_PREFIX;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
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
    private static final String FILTER="("+ENDPOINT_CONFIG_PREFIX+"=jax-ws)";
    //private static final String PROP_CXF_URL="org.cxf.url";
    
    @Before
    public void setUp() {
        super.setUp();
        ipojo.createComponentInstance("RoSe_machine");
        ipojo.createComponentInstance("RoSe_exporter.cxf");
    }
    
    @Configuration
	public static Option[] endpointCreatorBundle() {
    	
    	Option[] platform = options(systemPackages(
        				"javax.activation,"+
        				"javax.annotation,"+
        				"javax.imageio,"+
        				"javax.imageio.stream,"+
        				"javax.jws,"+
        				"javax.jws.soap,"+
        				"javax.management,"+
        				"javax.management.modelmbean,"+
        				"javax.management.remote,"+
        				"javax.naming,"+
        				"javax.net.ssl,"+
        				"javax.transaction.xa,"+
        				"javax.xml.bind,"+
        				"javax.xml.bind.annotation,"+
        				"javax.xml.bind.annotation.adapters,"+
        				"javax.xml.bind.attachment,"+
        				"javax.xml.datatype,"+
        				"javax.xml.namespace,"+
        				"javax.xml.parsers,"+
        				"javax.xml.soap,"+
        				"javax.xml.transform,"+
        				"javax.xml.transform.dom,"+
        				"javax.xml.transform.sax,"+
        				"javax.xml.transform.stream,"+
        				"javax.xml.validation,"+
        				"javax.xml.ws,"+
        				"javax.xml.ws.handler,"+
        				"javax.xml.ws.http,"+
        				"javax.xml.ws.soap,"+
        				"javax.xml.ws.spi,"+
        				"javax.xml.ws.wsaddressing,"+
        				"javax.xml.xpath,"+
        				"org.w3c.dom,"+
        				"org.w3c.dom.bootstrap,"+
        				"org.w3c.dom.ls,"+
        				"org.xml.sax,"+
        				"org.xml.sax.ext,"+
        				"org.xml.sax.helpers"
        				));
    	
    	Option[] bundles = options(CoreOptions.provision(
                mavenBundle().groupId("org.slf4j").artifactId("slf4j-api").versionAsInProject(),
				mavenBundle().groupId("org.slf4j").artifactId("slf4j-simple").versionAsInProject(),
                mavenBundle().groupId("javax.wsdl").artifactId("com.springsource.javax.wsdl").versionAsInProject(),
                mavenBundle().groupId("javax.mail").artifactId("com.springsource.javax.mail").versionAsInProject(),
                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.neethi").versionAsInProject(),
                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.xmlschema").versionAsInProject(),
                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.xmlresolver").versionAsInProject(),
                mavenBundle().groupId("commons-logging").artifactId("org.ow2.chameleon.commons.logging").versionAsInProject(),
                mavenBundle().groupId("org.apache.cxf").artifactId("org.ow2.chameleon.commons.cxf-minimal").versionAsInProject(),
                mavenBundle().groupId("com.sun.grizzly.osgi").artifactId("grizzly-httpservice-bundle").versionAsInProject(), 
                mavenBundle().groupId("org.ow2.chameleon.rose.ws").artifactId("cxf-exporter").versionAsInProject()
		));
		
    	return combine(platform, bundles);
		
	}

    @SuppressWarnings("unchecked")
	protected <T> T getProxy(ExportRegistration xreg,Class<T> itface) {
    	T proxy = null;
    	EndpointDescription description = xreg.getExportReference().getExportedEndpoint();

        try {
			ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
			factory.getInInterceptors().add(new LoggingInInterceptor());
			factory.getOutInterceptors().add(new LoggingOutInterceptor());
			factory.setServiceClass(itface);
			factory.setAddress("http://localhost:" + HTTP_PORT + "/jaxws/"+description.getId());
			proxy = (T) factory.create();
			
		} catch (Exception e) {
			fail(e.getMessage());
		}
    	
		return proxy;
	}
    
    protected ExporterService getExporterService(){
    	return rose.getServiceObject(ExporterService.class, FILTER);
    }
}

