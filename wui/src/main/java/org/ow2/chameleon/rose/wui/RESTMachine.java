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
    public Response getMachine(@PathParam("machineId")String machineId);

    @PUT
    @Path("{machineId}")
    public Response createMachine(@PathParam("machineId")String machineId,@QueryParam("host")String host);

    @DELETE
    @Path("{machineId}")
    public Response destroyMachine(@PathParam("machineId")String machineId);

    /*-----------
       Instance
     -----------*/

    @GET
    @Path("{machineId}/instances")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstances(@PathParam("machineId") String machineId);

    @GET
    @Path("{machineId}/instances/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstance(@PathParam("machineId") String machineId, @PathParam("name") String name);

    @PUT
    @Path("{machineId}/instances/{name}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createInstance(@PathParam("machineId") String machineId, @PathParam("name") String name, @QueryParam("component") String factory, String properties);

    @DELETE
    @Path("{machineId}/instances/{name}")
    public Response destroyInstance(@PathParam("machineId") String machineId, @PathParam("name") String name);


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
}
