package org.ow2.chameleon.rose.wui;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User: barjo
 * Date: 08/11/12
 * Time: 14:50
 */
@Path("/machine")
public interface RESTMachine {

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

    @GET
    @Path("{machineId}/export")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCreatedEndpoints(@PathParam("machineId") String machineId);

    @GET
    @Path("{machineId}/export/{endpointId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCreatedEndpoint(@PathParam("machineId") String machineId, @PathParam("endpointId") String endpointId);

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
}
