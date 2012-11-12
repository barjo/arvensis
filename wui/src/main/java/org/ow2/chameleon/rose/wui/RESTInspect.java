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

    @Path("/machines")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    Response getAllMachine(@QueryParam("filter")String filter);

    @Path("/machines/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    Response getMachine(@PathParam("id") String machineId);

    @Path("/exported")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    Response getExported(@QueryParam("filter")String filter,@QueryParam("machine")String machine);

    @Path("/exported/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    Response getExported(@PathParam("id")String endpoint);

}
