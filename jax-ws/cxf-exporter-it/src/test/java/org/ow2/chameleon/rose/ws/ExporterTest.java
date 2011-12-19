package org.ow2.chameleon.rose.ws;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ow2.chameleon.rose.RoSeConstants.ENDPOINT_CONFIG;
import static org.ow2.chameleon.rose.testing.RoSeHelper.waitForIt;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.RoSeConstants;
import org.ow2.chameleon.rose.testing.ExporterComponentAbstractTest;

/**
 * Integration test for the jabsorb-endpoint-creator component.
 * @author barjo
 */
@RunWith(JUnit4TestRunner.class)
public class ExporterTest extends ExporterComponentAbstractTest {
    private static final String FILTER="("+ENDPOINT_CONFIG+"=jax-ws)";
    
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
    
    /**
     * Test to export a jax-ws Service.
     */
    @Test
    public void testExportServiceWithJaxWs() {
    	//create a mock jaxws ws
    	HelloBob hello = mock(HelloBob.class);
    	
    	 //wait for the service to be available.
        waitForIt(100);
        
        ExporterService exporter = getExporterService(); //get the service
        
        //Register a mock LogService
        ServiceRegistration regLog = rose.registerService(hello,HelloBob.class);
        
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
        
        //Check that the ExportReference has been published
        ExportReference xref = rose.getServiceObject(ExportReference.class);
        
        //Check that the published ExportReference is equal to the ExportRegistration one
        assertEquals(xreg.getExportReference(), xref);
        
        //get a proxy
        HelloBob proxy = getProxy(xreg,HelloBob.class);
        
        //check proxy != null
        assertNotNull(proxy);
        
        //check proxy calls
        for (int i = 1; i <= MAX_MOCK; i++) {
            proxy.hello(); //call the proxy
            verify(hello,times(i)).hello(); //verify the mock
        }
    }
    

    @SuppressWarnings("unchecked")
	protected <T> T getProxy(ExportRegistration xreg,Class<T> itface) {
    	T proxy = null;
    	EndpointDescription description = xreg.getExportReference().getExportedEndpoint();

        try {
			ClientProxyFactoryBean factory;
			
			if (itface.isAnnotationPresent(WebService.class)){
				factory = new JaxWsProxyFactoryBean();
			}else {
				factory = new ClientProxyFactoryBean();
			}
			
			factory.getInInterceptors().add(new LoggingInInterceptor());
			factory.getOutInterceptors().add(new LoggingOutInterceptor());
			factory.setServiceClass(itface);
			factory.setAddress((String) description.getProperties().get(RoSeConstants.ENDPOINT_URL));
			proxy = (T) factory.create();
			
		} catch (Exception e) {
			fail(e.getMessage());
		}
    	
		return proxy;
	}
    
    protected ExporterService getExporterService(){
    	return rose.getServiceObject(ExporterService.class, FILTER);
    }
    
    /**
     * For test purpose
     * @author barjo
     */
    @WebService(name="hello")
    public interface HelloBob {
    	
    	@WebMethod(operationName="sayHi")
    	public String hello();
    }
}

