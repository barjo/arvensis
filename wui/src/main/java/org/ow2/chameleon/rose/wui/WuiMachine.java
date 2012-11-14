package org.ow2.chameleon.rose.wui;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.ow2.chameleon.rose.api.InConnection;
import org.ow2.chameleon.rose.api.Instance;
import org.ow2.chameleon.rose.api.Machine;
import org.ow2.chameleon.rose.api.OutConnection;

import javax.ws.rs.core.Response;
import java.util.*;

import static org.ow2.chameleon.rose.api.Machine.MachineBuilder.machine;

/**
 * Implementation of {@code RESTMachine} that allows for the management of RoSe machines over the web. It use The RoSe
 * Java fluent API {@link Machine}.
 */
public class WuiMachine implements RESTMachine {

    //Keep the resource registration
    private ServiceRegistration reg;

    private final BundleContext context;

    //Store the Machine created through this API.
    private final Map<String,Machine> myMachines = new HashMap<String, Machine>();

    public WuiMachine(BundleContext context) {
        this.context = context;
    }

    /**
     * Register the resource.
     */
    public void register(){
        if (reg!=null)
            reg.unregister();

        //Register the resource as a service, so that it can be exported.
        reg = context.registerService(RESTMachine.class.getName(),this,
                      new Hashtable(Collections.singletonMap("endpoint.id","RoSe-REST-Machine")));
    }

    /**
     * Unregister the resource
     */
    public void unRegister(){
        reg.unregister();
        reg=null;
    }

    /*-------------------------------------------------------
      Machine
        GET               /machines/:machineId      (json)
        GET, PUT, DELETE  /machines/:machineId      (json)
    ---------------------------------------------------------*/

    public Response getMachines(String filter) {
        JSONArray machines = new JSONArray();

        for(String id: myMachines.keySet()){
            machines.put(id);
        }

        return Response.ok(machines.toString()).build();
    }

    public Response getMachine(String machineId){
        if(!myMachines.containsKey(machineId))
            return Response.status(404).build();

        try{
            Machine machine = myMachines.get(machineId);
            JSONObject json = new JSONObject(machine.getConf());
            json.put("state",machine.getState());

            return Response.ok(json.toString()).build();
        } catch (Exception e){
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


    public Response destroyMachine(String machineId) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }

        Machine m = myMachines.remove(machineId);
        m.stop();

        return Response.ok().build();
    }

    /*--------------------------------------------------------------------
      Instances
        GET               /machines/:machineId/instances          (json)
        GET, PUT, DELETE  /machines/:machineId/instances/:instId  (json)
    ----------------------------------------------------------------------*/


    public Response createInstance(String machineId, String name, String factory, String properties) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }
        if (factory==null){
            return Response.status(400).entity("The request must contain the query param component").build();
        }

        Map props;

        try {
            props = toJson(properties);
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

    public Response getInstance(String machineId, String name) {
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

    /*---------------------------------------------------------------
       OutConnection
         GET               /machines/:machineId/outs         (json)
         GET, PUT, DELETE  /machines/:machineId/outs/:outId  (json)
     ----------------------------------------------------------------*/

    public Response createOut(String machineId, String name, String serviceFilter, String properties) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }
        if (serviceFilter==null){
            return Response.status(400).entity("The request must contain the query param service_filter").build();
        }

        Map props;

        try {
            props = toJson(properties);
        } catch (JSONException e) {
            return Response.status(400).entity(e.getMessage()).build();
        }


        Machine m = myMachines.get(machineId);

        try {
            m.out(serviceFilter).withProperties(props).withProperty("connection.id", name).add();
        } catch (InvalidSyntaxException e){
            return Response.status(400).entity("the param service_filter is not valid:" + e.getMessage()).build();
        }
        m.start();

        return Response.ok().build();
    }

    public Response destroyOut(String machineId, String name) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }

        Machine m = myMachines.get(machineId);
        OutConnection todestroy = null;
        for (OutConnection out: m.getOuts()){
            if (name.equals(out.getConf().get("connection.id"))){
                todestroy = out;
                break;
            }
        }

        if(todestroy!=null){
            m.remove(todestroy);
        }

        return Response.ok().build();
    }

    public Response getOuts(String machineId) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" has not been created through the wui!").build();
        }

        JSONArray json = new JSONArray();
        List<OutConnection> outs = myMachines.get(machineId).getOuts();

        for(OutConnection out: outs){
            json.put(out.getConf().get("connection.id"));
        }

        return Response.ok(json.toString()).build();
    }

    public Response getOut(String machineId, String name) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }

        JSONObject json = null;
        List<OutConnection> outs = myMachines.get(machineId).getOuts();

        for(OutConnection out: outs){
            if (name.equals(out.getConf().get("connection.id"))){
                json = new JSONObject(out.getConf());
                try{ json.put("size",out.size()); }catch (JSONException e){};
                break;
            }
        }

        if (json == null){
            return Response.status(400).entity("Out Connection: " + name + " does not exist for Machine " + machineId).build();
        }

        return Response.ok(json.toString()).build();
    }

    /*---------------------------------------------------------------
      InConnection
        GET               /machines/:machineId/ins          (json)
        GET, PUT, DELETE  /machines/:machineId/ins/:inId    (json)
    ----------------------------------------------------------------*/

    public Response createIn(String machineId, String name, String endpointFilter, String properties) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }
        if (endpointFilter==null){
            return Response.status(400).entity("The request must contain the query param endpoint_filter").build();
        }

        Map props;

        try {
            props = toJson(properties);
        } catch (JSONException e) {
            return Response.status(400).entity(e.getMessage()).build();
        }


        Machine m = myMachines.get(machineId);

        try {
            m.in(endpointFilter).withProperties(props).withProperty("connection.id", name).add();
        } catch (InvalidSyntaxException e){
            return Response.status(400).entity("the param endpoint_filter is not valid:" + e.getMessage()).build();
        }
        m.start();

        return Response.ok().build();
    }

    public Response destroyIn(String machineId, String inId) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }

        Machine m = myMachines.get(machineId);
        InConnection todestroy = null;
        for (InConnection in: m.getIns()){
            if (inId.equals(in.getConf().get("connection.id"))){
                todestroy = in;
                break;
            }
        }

        if(todestroy!=null){
            m.remove(todestroy);
        }

        return Response.ok().build();
    }

    public Response getIns(String machineId) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" has not been created through the wui!").build();
        }

        JSONArray json = new JSONArray();
        List<InConnection> ins = myMachines.get(machineId).getIns();

        for(InConnection in: ins){
            json.put(in.getConf().get("connection.id"));
        }

        return Response.ok(json.toString()).build();
    }

    public Response getIn(String machineId, String inId) {
        if(!myMachines.containsKey(machineId)){
            return Response.status(404).entity("Machine "+machineId+" does not exist").build();
        }

        JSONObject json = null;
        List<InConnection> ins = myMachines.get(machineId).getIns();

        for(InConnection in: ins){
            if (inId.equals(in.getConf().get("connection.id"))){
                json = new JSONObject(in.getConf());
                try{ json.put("size",in.size()); }catch (JSONException e){};
                break;
            }
        }

        if (json == null){
            return Response.status(400).entity("In Connection: " + inId + " does not exist for Machine " + machineId).build();
        }

        return Response.ok(json.toString()).build();
    }



    public static Map<String,Object> toJson(String json) throws JSONException {
        Map<String,Object> map = new HashMap<String, Object>();

        if(json==null || json.equals(""))
            return map;

        JSONObject jobj = new JSONObject(json);

        for(Iterator<String> iter = jobj.keys(); iter.hasNext();){
            String key = iter.next();
            map.put(key,jobj.get(key));
        }

        return map;
    }
}
