package org.ow2.chameleon.rose.wui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.*;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.ow2.chameleon.rose.RoseMachine;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

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

    /**
     * @return 200, is alive.
     */
    public Response ping() {
        return Response.ok().build();
    }

    public Response getAllMachine(@QueryParam("filter") String filter) {
        try {
            filter = (filter == null ? "("+ RoseMachine.RoSe_MACHINE_ID+"=*)" : "(&("+RoseMachine.RoSe_MACHINE_ID+"=*)"+filter+")");
            ServiceReference[] refs = context.getServiceReferences(RoseMachine.class.getName(), filter);

            JSONArray machines = new JSONArray();

            if (refs != null) {
                for (ServiceReference ref: refs){
                    RoseMachine machine = (RoseMachine) context.getService(ref);
                    JSONObject json = new JSONObject(machine.getProperties());
                    machines.put(json);
                    context.ungetService(ref);
                }
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

    public Response getDiscovered(@QueryParam("machine") String machineId) {
        try{
            ServiceReference[] refs = context.getServiceReferences(RoseMachine.class.getName(), "("+RoseMachine.RoSe_MACHINE_ID+"="+(machineId == null ? "*" : machineId)+")");
            JSONArray discovered = new JSONArray();


            if (!(refs == null)){     //machines available
                Set<EndpointDescription> buffer = new HashSet<EndpointDescription>();

                for(ServiceReference ref: refs){
                    buffer.addAll(((RoseMachine) context.getService(ref)).getDiscoveredEndpoints());
                    context.ungetService(ref);
                }

                for(EndpointDescription desc: buffer){
                    discovered.put(new JSONObject(desc.getProperties()));
                }
            }


            return Response.ok().entity(discovered.toString()).build();

        }catch (InvalidSyntaxException e){
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    public Response getExported(@QueryParam("machine") String machine) {
        String myfilter = "(endpoint.framework.uuid="+ ((machine==null) ? "*" : machine) +")";

        try {
            ServiceReference[] refs = context.getServiceReferences(ExportReference.class.getName(), myfilter);

            JSONArray endpoints = new JSONArray();

            if (refs != null){
                for(ServiceReference ref: refs){
                    ExportReference expref = (ExportReference) context.getService(ref);
                    endpoints.put(new JSONObject(expref.getExportedEndpoint().getProperties()));
                    context.ungetService(ref);
                }
            }

            return  Response.ok(endpoints.toString()).build();

        }catch (InvalidSyntaxException e){
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    public Response getImported(@QueryParam("framework") String machineId) {
        try{
            ServiceReference[] refs = context.getServiceReferences(RoseMachine.class.getName(), "("+RoseMachine.RoSe_MACHINE_ID+"="+(machineId == null ? "*" : machineId)+")");
            JSONArray imported = new JSONArray();


            if (!(refs == null)){     //machines available
                Set<ImportReference> buffer = new HashSet<ImportReference>();

                for(ServiceReference ref: refs){
                    buffer.addAll(((RoseMachine) context.getService(ref)).getImportedEndpoints());
                    context.ungetService(ref);
                }

                for(ImportReference imref: buffer){
                    JSONObject json = new JSONObject(imref.getImportedEndpoint().getProperties());
                    try{ json.put("service",imref.getImportedService().getProperty(Constants.SERVICE_ID)); }catch (JSONException e){}
                    imported.put(json);
                }
            }


            return Response.ok().entity(imported.toString()).build();

        }catch (InvalidSyntaxException e){
            return Response.status(400).entity(e.getMessage()).build();
        }
    }
}
