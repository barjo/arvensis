package org.ow2.chameleon.rose.rest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.felix.ipojo.ComponentInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.ow2.chameleon.rose.api.Instance;
import org.ow2.chameleon.rose.api.Machine;
import org.ow2.chameleon.rose.api.OutConnection;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ow2.chameleon.rose.api.Machine.MachineBuilder.machine;
import static org.ow2.chameleon.rose.util.RoseTools.waitForIt;

/**
 * Integration test for the jersey-exporter component.
 * @author barjo
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class JerseyExporterTest  {
    private static final String HTTP_PORT = "9027";


    @Inject
    BundleContext context;

    Instance myExporter;
    Machine myMachine;

    @Before
    public void setUp() {
        myMachine = machine(context,"test-machine").host("localhost").create();
        myExporter = myMachine.exporter("RoSe_exporter.jersey").withProperty("jersey.servlet.name","/rest").create();
        myMachine.start();
    }

    @After
    public void tearDown(){
        myMachine.stop();
    }
    
    
    @Configuration
	public static Option[] endpointCreatorBundle() {

        return options(
                //config
                systemProperty( "org.osgi.service.http.port" ).value( HTTP_PORT ),
                //bundles
                junitBundles(),
                provision(
                //ipojo
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.ipojo").versionAsInProject(),
                //rose
                mavenBundle().groupId("org.ow2.chameleon.rose").artifactId("rose-core").versionAsInProject(),
                mavenBundle().groupId("org.ow2.chameleon.rose.rest").artifactId("jersey-exporter").versionAsInProject(),
                //httpService & dependencies
                mavenBundle().groupId("com.sun.grizzly.osgi").artifactId("grizzly-httpservice-bundle").versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-server").versionAsInProject(),
                mavenBundle().groupId("com.sun.jersey").artifactId("jersey-client").versionAsInProject(),
                mavenBundle().groupId("com.sun.jersey").artifactId("jersey-core").versionAsInProject(),
                mavenBundle().groupId("javax.mail").artifactId("mail").versionAsInProject(),
                mavenBundle().groupId("javax.xml.bind").artifactId("jaxb-api-osgi").versionAsInProject()
		));
	}

    /**
     * Test that RoSe_exporter.jersey is successfully created
     */
    @Test
    public void testExporterCreation(){
        waitForIt(3000);
        assertTrue(myExporter.getState() == ComponentInstance.VALID);
    }

    /**
     * Test that RoSe_exporter.jersey is successfully created
     */
    @Test
    public void testResourceExport(){
        try{
            ServiceRegistration reg = context.registerService(DummyResource.class.getName(), new DummyResource(), null);
            OutConnection out = myMachine.out("(" + Constants.OBJECTCLASS + "=" + DummyResource.class.getName() + ")").add();
            //Check that the resource has been exported
            assertTrue(out.size() == 1);
            out.close();
            reg.unregister();
        }catch (InvalidSyntaxException is){
            fail(is.getMessage());
        }
    }

    /**
     * Test that RoSe_exporter.jersey destroy properly the endpoint
     */
    @Test
    public void testResourceStopExport(){
        try{
            ServiceRegistration reg = context.registerService(DummyResource.class.getName(), new DummyResource(), null);
            OutConnection out = myMachine.out("(" + Constants.OBJECTCLASS + "=" + DummyResource.class.getName() + ")").add();
            reg.unregister();
            //Check that the resource is no more exported
            assertTrue(out.size() == 0);
            out.close();
        }catch (InvalidSyntaxException is){
            fail(is.getMessage());
        }
    }

    /**
     * Test that the resource can be call
     */
    @Test
    public void testResourceExportAndCall(){
        try{
            ServiceRegistration reg = context.registerService(DummyResource.class.getName(), new DummyResource(), null);
            OutConnection out = myMachine.out("(" + Constants.OBJECTCLASS + "=" + DummyResource.class.getName() + ")").add();

            //Call the ressource
            String hello = getProxy().get(String.class);
            assertEquals(hello,"hello");

            //close
            out.close();
            reg.unregister();
        }catch (InvalidSyntaxException is){
            fail(is.getMessage());
        }
    }

    /**
     * Test that the resource cannot be called any more if the service has been unregistered.
     */
    @Test
    public void testResourceStopExportAndCallFail(){
        try{
            ServiceRegistration reg = context.registerService(DummyResource.class.getName(), new DummyResource(), null);
            OutConnection out = myMachine.out("(" + Constants.OBJECTCLASS + "=" + DummyResource.class.getName() + ")").add();

            //unregister the resource
            reg.unregister();

            //Check that the resource has been exported
            ClientResponse response = getProxy().get(ClientResponse.class);
            assertTrue(response.getStatus() == 404);

            //close
            out.close();

        }catch (InvalidSyntaxException is){
            fail(is.getMessage());
        }
    }

    /**
     * @return The WebResource, proxy of the exported service.
     */
    public WebResource getProxy(){
        Client client = Client.create();
        return client.resource("http://localhost:"+HTTP_PORT+"/rest/hello");
    }

    @Path("/hello")
    public class DummyResource {

        @GET
        public Response getHello(){
            return Response.ok("hello").build();
        }
    }
}

