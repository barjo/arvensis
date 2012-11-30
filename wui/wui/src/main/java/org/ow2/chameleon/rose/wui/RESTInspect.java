package org.ow2.chameleon.rose.wui;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * User: barjo
 * Date: 12/11/12
 * Time: 10:03
 */
@Path("/inspect")
public interface RESTInspect {

    @OPTIONS
    Response ping();

    @Path("/machines")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    Response getAllMachine(@QueryParam("filter")String filter);

    @Path("/machines/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    Response getMachine(@PathParam("id") String machineId);

    @Path("/discovered")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response getDiscovered(@QueryParam("machine")String machineId);

    @Path("/exported")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    Response getExported(@QueryParam("machine")String machineId);

    @Path("/imported")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    Response getImported(@QueryParam("framework")String machineId);

}
