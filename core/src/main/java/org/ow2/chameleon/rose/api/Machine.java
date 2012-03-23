package org.ow2.chameleon.rose.api;

import static java.util.Collections.unmodifiableList;
import static org.apache.felix.ipojo.Factory.VALID;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.ow2.chameleon.rose.RoSeConstants.RoSe_MACHINE_COMPONENT_NAME;
import static org.ow2.chameleon.rose.RoseMachine.RoSe_MACHINE_HOST;
import static org.ow2.chameleon.rose.RoseMachine.RoSe_MACHINE_ID;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public final class Machine {
	private static final String RoSe_FACTORY_FILTER="(&("+OBJECTCLASS+"="+Factory.class.getName()+")(factory.state="+VALID+")(factory.name="+RoSe_MACHINE_COMPONENT_NAME+"))";

	private volatile Boolean started = false;
	private final BundleContext context;
	private final ServiceTracker tracker;
	private final List<InConnection> ins = new ArrayList<InConnection>();
	private final List<OutConnection> outs = new ArrayList<OutConnection>();
	private final List<Instance> instances = new ArrayList<Instance>();

	private Hashtable<String, Object> conf = new Hashtable<String, Object>();

	private Machine(MachineBuilder builder) {	
		context = builder.context;
		
		try {
			tracker = new ServiceTracker(context, context.createFilter(RoSe_FACTORY_FILTER), new RoSeMachineCreator());
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("Bad filter", e); //impossible
		}
		conf.put(RoSe_MACHINE_ID, builder.id);
		conf.put("instance.name", RoSe_MACHINE_COMPONENT_NAME+"_"+builder.id);
		conf.put(RoSe_MACHINE_HOST, builder.host);
	}
	
	/**
	 * Start the rose machine, open all connections and start each instances. 
	 */
	public void start(){
		tracker.open(false);
		
		for (InConnection in : ins) {
			in.open();
		}
		for (OutConnection out : outs) {
			out.open();
		}
		for (Instance in : instances) {
			in.start();
		}
		
		started = true;
	}
	
	/**
	 * Stop the rose machine, close all connections and stop each instances.
	 */
	public void stop(){
		tracker.close();
		
		for (InConnection in : ins) {
			in.close();
		}
		for (OutConnection out : outs) {
			out.close();
		}
		for (Instance in : instances) {
			in.stop();
		}
		
		started = false;
	}
	
	/**
	 * add a In connection.
	 * @param in
	 */
	protected void add(InConnection in){
		ins.add(in);
		
		if (started){
			in.open();
		}
	}
	
	/**
	 * Remove and close the connection.
	 * @param in
	 */
	public void remove(InConnection in){
		ins.remove(in);
		in.close();
	}
	
	/**
	 * Get the In connections.
	 * @return
	 */
	public List<InConnection> getIns(){
		return unmodifiableList(ins);
	}
	
	/**
	 * Remove and close the connection.
	 * @param out
	 */
	public void remove(OutConnection out){
		outs.remove(out);
		out.close();
	}
	
	/**
	 * add an out connection
	 * @param out
	 */
	protected void add(OutConnection out){
		outs.add(out);

		if (started){
			out.open();
		}
	}
	
	/**
	 * Get the Out connections.
	 * @return
	 */
	public List<OutConnection> getOuts(){
		return unmodifiableList(outs);
	}
	
	/**
	 * add a Component instance
	 * @param in
	 */
	protected void add(Instance in){
		instances.add(in);
		
		if (started){
			in.start();
		}
	}
	
	/**
	 * Remove and close the component instance.
	 * @param in
	 */
	public void remove(Instance in){
		instances.remove(in);
		in.stop();
	}
	
	/**
	 * Get the Component Instances.
	 * @return
	 */
	public List<Instance> getInstances(){
		return unmodifiableList(instances);
	}
	
	
	/**
	 * Get the bundleContext associated with this RoSeMachine.
	 * @return
	 */
	public BundleContext getContext(){
		return context;
	}
	
	
	/**
	 * @return The RoSeMachine id.
	 */
	public String getId() {
		return (String) conf.get(RoSe_MACHINE_ID);
	}
	
	
	/**
	 * Builder to create a RoSeMachine.
	 * @author barjo
	 */
	public static class MachineBuilder{
		private final BundleContext context;
		private final String id;
		private String host = "localhost";
		
		private MachineBuilder(BundleContext pContext,String pId) {
			context = pContext;
			id=pId;
		}
		
		public static MachineBuilder machine(BundleContext pContext,String pId){
			return new MachineBuilder(pContext, pId);
		}
		
		public MachineBuilder host(String pHost){
			host=pHost;
			return this;
		}
		
		public Machine create(){
			return new Machine(this);
		}
	}
	
	
	/**
	 * Get the RoSe_machine component factory and create an instance if binded.
	 * @author barjo
	 */
	private class RoSeMachineCreator implements ServiceTrackerCustomizer {
		
		public Object addingService(ServiceReference reference) {
			Factory factory = (Factory) context.getService(reference);
			context.ungetService(reference);
			try {
				return factory.createComponentInstance(conf);
			} catch (Exception e) {
				return null;
			}
		}

		public void modifiedService(ServiceReference reference, Object service) {
		}

		public void removedService(ServiceReference reference, Object service) {
			((ComponentInstance) service).dispose();
		}
		
	}
}
