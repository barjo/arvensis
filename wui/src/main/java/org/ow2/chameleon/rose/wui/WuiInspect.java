package org.ow2.chameleon.rose.wui;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.ow2.chameleon.rose.RoseMachine;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Hashtable;

/**
 * User: barjo
 * Date: 12/11/12
 * Time: 10:03
 */
public class WuiInspect implements  RESTInspect{

    private ServiceRegistration reg;

    private final BundleContext context;

    public WuiInspect(BundleContext context) {
        this.context = context;
    }

    public void register(){
        if (reg!=null)
            reg.unregister();

        reg = context.registerService(RESTInspect.class.getName(),this, new Hashtable(Collections.singletonMap("endpoint.id","RoSe-REST-Inspect")));
    }

    public void unRegister(){
        reg.unregister();
        reg=null;
    }

    public Response getAllMachine(@QueryParam("filter") String filter) {
        try {
            filter = (filter == null ? "("+ RoseMachine.RoSe_MACHINE_ID+"=*)" : "(&("+RoseMachine.RoSe_MACHINE_ID+"=*)"+filter+")");
            ServiceReference[] refs = context.getServiceReferences(RoseMachine.class.getName(), filter);

            JSONArray machines = new JSONArray();

            if (refs != null) {
                for (ServiceReference ref: refs)
                    machines.put(ref.getProperty(RoseMachine.RoSe_MACHINE_ID));
            }

            return Response.ok(machines.toString()).build();

        } catch (InvalidSyntaxException e) { //bad filter
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    public Response getMachine(@PathParam("id") String machineId) {
        try {
            ServiceReference[] refs = context.getServiceReferences(RoseMachine.class.getName(), "("+RoseMachine.RoSe_MACHINE_ID+"="+machineId+")");

            if (refs == null){
                return Response.noContent().status(404).build();
            } else {
                RoseMachine machine = (RoseMachine) context.getService(refs[0]);
                context.ungetService(refs[0]);

                return Response.ok((new JSONObject(machine.getProperties())).toString()).build();
            }

        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    public Response getExported(@QueryParam("filter") String filter, @QueryParam("machine") String machine) {
        String myfilter = "(&" + ((filter==null) ? "" : filter) +"(endpoint.framework.uuid="+ ((machine==null) ? "*" : machine) +"))";

        try {
            ServiceReference[] refs = context.getServiceReferences(ExportReference.class.getName(), myfilter);

            JSONArray endpoints = new JSONArray();

            if (refs != null){
                for(ServiceReference ref: refs){
                    endpoints.put(ref.getProperty("endpoint.id"));
                }
            }

            return  Response.ok(endpoints.toString()).build();

        }catch (InvalidSyntaxException e){
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    @Override
    public Response getExported(@PathParam("id") String endpoint) {
        try {
            ServiceReference[] refs = context.getServiceReferences(ExportReference.class.getName(), "(endpoint.id="+endpoint+")");
            if (refs==null)
                return Response.status(404).build();

            ExportReference expref = (ExportReference) context.getService(refs[0]);

            JSONObject json = new JSONObject(expref.getExportedEndpoint().getProperties());

            return  Response.ok(json.toString()).build();

        }catch (InvalidSyntaxException e){
            return Response.status(400).entity(e.getMessage()).build();
        }
    }
}
