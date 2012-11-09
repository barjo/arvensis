package org.ow2.chameleon.rose.wui;

import org.apache.felix.ipojo.annotations.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.api.Instance;
import org.ow2.chameleon.rose.api.Machine;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.ow2.chameleon.rose.api.Machine.MachineBuilder.machine;

/**
 * User: barjo
 * Date: 05/11/12
 * Time: 14:36
 */
@Component(name="RoSe_Wui")
@Instantiate
@Provides(specifications = RESTMachine.class)
public class WuiComp implements RESTMachine {

    @Requires(optional = true)
    private LogService logger;

    private final Machine myrose;

    private final BundleContext context;

    private final Map<String,Machine> myMachines = new HashMap<String, Machine>();

    public WuiComp(BundleContext context) throws InvalidSyntaxException {
        this.context = context;

        //Create a rose machine that export this service thx to jersey.
        myrose = machine(context,"wui-machine").create();
        myrose.exporter("RoSe_exporter.jersey").withProperty("jersey.servlet.name","/rose").create();
        myrose.out("("+ Constants.OBJECTCLASS+"="+RESTMachine.class.getName()+")").protocol(Collections.singletonList("jax-rs")).add();
    }

    public Response getMachines(String filter) {
        try {
            filter = (filter == null ? "("+RoseMachine.RoSe_MACHINE_ID+"=*)" : "(&("+RoseMachine.RoSe_MACHINE_ID+"=*)"+filter+")");
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

    public Response getMachine(String machineId){
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
          logger.log(LogService.LOG_DEBUG,"Cannot get the RoSeMachine available on the gateway",e);
          return Response.serverError().build();
        }
    }

    public Response getCreatedEndpoints(String machineId) {
        try {

            ServiceReference[] refs = context.getServiceReferences(ExportReference.class.getName(), "(endpoint.framework.uuid="+machineId+")");
            if (refs == null){
                return Response.noContent().status(404).build();
            }

            JSONArray endpoints = new JSONArray();

            for(ServiceReference ref: refs){
                endpoints.put(ref.getProperty("endpoint.id"));
            }

            return  Response.ok(endpoints.toString()).build();

        }catch (InvalidSyntaxException e){
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    public Response getCreatedEndpoint(String machineId, String endpointId) {
        System.out.println("PIFEnd");
        try {

            ServiceReference[] refs = context.getServiceReferences(ExportReference.class.getName(),
                    "(&(endpoint.framework.uuid="+machineId+")(endpoint.id="+endpointId+"))");

            if (refs == null){
                return Response.noContent().status(404).build();
            }

            else {
                ExportReference endpoint = (ExportReference) context.getService(refs[0]);
                context.ungetService(refs[0]);

                return Response.ok((new JSONObject(endpoint.getExportedEndpoint().getProperties())).toString()).build();
            }

        }catch (InvalidSyntaxException e){
            return Response.status(400).entity(e.getMessage()).build();
        }
    }

    public Response createMachine(String machineId,String host) {
        if(myMachines.containsKey(machineId)){
            return Response.status(400).entity("Machine "+machineId+" has already been created").build();
        }

        Machine m;
        if (host == null)
            m = machine(context,machineId).create();
        else
            m = machine(context,machineId).host(host).create();

        myMachines.put(m.getId(),m);
        m.start();

        return Response.ok().build();
    }

    public Response createInstance(String machineId, String name, String factory, String properties) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }
        if (factory==null){
            return Response.status(400).entity("The request must contain the query param factory").build();
        }

        Map<String,Object> props;

        try {
            props=toJson(properties);
        } catch (JSONException e) {
            return Response.status(400).entity(e.getMessage()).build();
        }


        Machine m = myMachines.get(machineId);

        m.exporter(factory).name(name).withProperties(props).create();

        m.start();

        return Response.ok().build();
    }

    public Response destroyInstance(String machineId, String name) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }

        Machine m = myMachines.get(machineId);
        Instance todestroy = null;
        for (Instance instance: m.getInstances()){
            if (name.equals(instance.getConf().get("instance.name"))){
                todestroy = instance;
                break;
            }
        }

        if(todestroy!=null){
            m.remove(todestroy);
        }

        return Response.ok().build();
    }

    public Response getInstances(String machineId) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" has not been created through the wui!").build();
        }

        JSONArray json = new JSONArray();
        List<Instance> instances = myMachines.get(machineId).getInstances();

        for(Instance instance: instances){
            json.put(instance.getConf().get("instance.name"));
        }

        return Response.ok(json.toString()).build();
    }

    @Override
    public Response getInstance(@PathParam("machineId") String machineId, @PathParam("name") String name) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }

        JSONObject json = null;
        List<Instance> instances = myMachines.get(machineId).getInstances();

        for(Instance instance: instances){
            if (name.equals(instance.getConf().get("instance.name"))){
                json = new JSONObject(instance.getConf());
                try {
                    json.accumulate("component", instance.getComponent());
                    json.accumulate("state", instance.getState());
                } catch (JSONException e) {}
                break;
            }
        }

        if (json == null){
            return Response.status(400).entity("Instance: " + name + " does not exist for Machine " + machineId).build();
        }

        return Response.ok(json.toString()).build();
    }

    public Response destroyMachine(String machineId) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }

        Machine m = myMachines.remove(machineId);
        m.stop();

        return Response.ok().build();
    }

    @Validate
    private void start() {
        myrose.start();
    }

    @Invalidate
    private void stop() {
        myrose.stop();
    }

    private Map<String,Object> toJson(String json) throws JSONException {
        Map<String,Object> map = new HashMap<String, Object>();

        if(json==null)
            return map;

        JSONObject jobj = new JSONObject(json);

        for(Iterator<String> iter = jobj.keys(); iter.hasNext();){
            String key = iter.next();
            map.put(key,jobj.get(key));
        }

        return map;
    }
}