package org.ow2.chameleon.rose.wui;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User: barjo
 * Date: 08/11/12
 * Time: 14:50
 */
@Path("/machines")
public interface RESTMachine {

    /*-----------
       Machine
     -----------*/

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response getMachines(@QueryParam("filter") String filter);

    @GET
    @Path("{machineId}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getMachine(@PathParam("machineId")String machineId);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Response createMachine(String machineJson);

    @DELETE
    @Path("{machineId}")
    Response destroyMachine(@PathParam("machineId")String machineId);

    /*-----------
       Instance
     -----------*/

    @GET
    @Path("{machineId}/instances")
    @Produces(MediaType.APPLICATION_JSON)
    Response getInstances(@PathParam("machineId") String machineId);

    @POST
    @Path("{machineId}/instances")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    Response postInstance(String machineId, String content);

    @GET
    @Path("{machineId}/instances/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    Response getInstance(@PathParam("machineId") String machineId, @PathParam("name") String name);

    @PUT
    @Path("{machineId}/instances/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    Response putInstance(@PathParam("machineId") String machineId, @PathParam("name") String name, @QueryParam("component") String factory, String properties);

    @DELETE
    @Path("{machineId}/instances/{name}")
    Response destroyInstance(@PathParam("machineId") String machineId, @PathParam("name") String name);


    /*---------------
       OutConnection
    -----------------*/

    @GET
    @Path("{machineId}/outs")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOuts(@PathParam("machineId") String machineId);

    @GET
    @Path("{machineId}/outs/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOut(@PathParam("machineId") String machineId, @PathParam("id") String name);

    @PUT
    @Path("{machineId}/outs/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOut(@PathParam("machineId") String machineId, @PathParam("id") String name,@QueryParam("service_filter") String serviceFilter, String properties);

    @DELETE
    @Path("{machineId}/outs/{id}")
    public Response destroyOut(@PathParam("machineId") String machineId, @PathParam("id") String name);

    /*---------------
       InConnection
    -----------------*/

    @GET
    @Path("{machineId}/ins")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIns(@PathParam("machineId") String machineId);

    @GET
    @Path("{machineId}/ins/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIn(@PathParam("machineId") String machineId, @PathParam("id") String inId);

    @PUT
    @Path("{machineId}/ins/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createIn(@PathParam("machineId") String machineId, @PathParam("id") String inId,@QueryParam("endpoint_filter") String endpointFilter, String properties);

    @DELETE
    @Path("{machineId}/ins/{id}")
    public Response destroyIn(@PathParam("machineId") String machineId, @PathParam("id") String inId);
}
