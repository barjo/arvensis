package org.ow2.chameleon.rose.wui.test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import org.ow2.chameleon.rose.RoseMachine;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ow2.chameleon.rose.RoseMachine.RoSe_MACHINE_DATE;
import static org.ow2.chameleon.rose.util.RoseTools.waitForIt;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class RESTInspectTest {

    private static final String HTTP_PORT = "9027";


    @Inject
    BundleContext context;

    @Inject
    RoseMachine machine;

    @Before
    public void setUp() {

    }

    @After
    public void tearDown(){
    }


    @Configuration
    public static Option[] endpointCreatorBundle() {

        return options(
                //config
                systemProperty( "org.osgi.service.http.port" ).value(HTTP_PORT),
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
                        mavenBundle().groupId("javax.xml.bind").artifactId("jaxb-api-osgi").versionAsInProject(),
                        //tested project
                        mavenBundle().groupId("org.ow2.chameleon.rose").artifactId("rose-wui").versionAsInProject()
                ));
    }

    /**
     * Test that RoSe_exporter.jersey is successfully created
     */
    @Test
    public void testRestInspectExport(){
       waitForIt(1000);
       //ping
       assertTrue(ping(200));
    }

    /**
     * Test GET /machines
     */
    @Test
    public void testInspectMachine(){
        ClientResponse response = doGet("/machines");
        //HTTP OK
        assertTrue(response.getStatus() == 200);
        //Has content
        assertTrue(response.hasEntity());
        //Content is json
        assertEquals(response.getType(), MediaType.APPLICATION_JSON_TYPE);

        try {
            JSONArray content = new JSONArray(response.getEntity(String.class));
            assertTrue(content.length()==1);
            assertEquals(content.getJSONObject(0).getString("id"),machine.getId());
            assertEquals(content.getJSONObject(0).getString("host"),machine.getHost());
        } catch (JSONException e) {
            fail(e.getMessage());
        }
    }

    /**
     * Test GET /machines/:id
     */
    @Test
    public void testInspectMachineId(){
        ClientResponse response = doGet("/machines/"+machine.getId());
        //HTTP OK
        assertTrue(response.getStatus() == 200);
        //Has content
        assertTrue(response.hasEntity());
        //Content is json
        assertEquals(response.getType(), MediaType.APPLICATION_JSON_TYPE);

        try {
            JSONObject content = new JSONObject(response.getEntity(String.class));

            assertEquals(content.getString("id"),machine.getId());
            assertEquals(content.getString("host"),machine.getHost());
            assertEquals(content.getLong("date"),machine.getProperties().get(RoSe_MACHINE_DATE));
        } catch (JSONException e) {
            fail(e.getMessage());
        }
    }



    /**
     * @return The WebResource, proxy of the exported service.
     */
    public WebResource getProxy(String path){
        Client client = Client.create();
        return client.resource("http://localhost:"+HTTP_PORT+"/rose/inspect"+path);
    }

    /**
     * @return The GET, ClientResponse
     */
    public ClientResponse doGet(String path){
        return getProxy(path).get(ClientResponse.class);
    }

    /**
     * @return <code>true</code> if the pong status is equal to <code>status</code>
     * @param status the expected http status
     */
    public boolean ping(int status){
        return (getProxy("").options(ClientResponse.class).getStatus() == status);
    }
}