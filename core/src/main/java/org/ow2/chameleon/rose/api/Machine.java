package org.ow2.chameleon.rose.api;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.api.InConnection.InBuilder;
import org.ow2.chameleon.rose.api.Instance.InstanceBuilder;
import org.ow2.chameleon.rose.api.OutConnection.OutBuilder;

import java.util.*;

import static org.apache.felix.ipojo.Factory.VALID;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.ow2.chameleon.rose.RoSeConstants.RoSe_MACHINE_COMPONENT_NAME;
import static org.ow2.chameleon.rose.RoseMachine.RoSe_MACHINE_HOST;
import static org.ow2.chameleon.rose.RoseMachine.RoSe_MACHINE_ID;

/**
 * The meat of RoSe. Once you have created a RoSe machine you can add to
 * {@link InConnection} that allows for the import of remote services and
 * {@link OutConnection} that allows for the export of local OSGi services. You
 * can also create {@link Instance} from the {@link Machine}, it's useful to
 * Instantiate component providing {@link ExporterService} and
 * {@link ImporterService} as well as discovery component.
 * 
 * @author barjo
 */
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
	 * Create an {@link InConnection} for this machine.
	 * @throws InvalidSyntaxException
	 */
	public InBuilder in(String descriptionFilter) throws InvalidSyntaxException{
		return InBuilder.in(this, descriptionFilter);
	}
	
	/**
	 * Create an {@link OutConnection} for this machine.
	 * @throws InvalidSyntaxException
	 */
	public OutBuilder out(String serviceFilter) throws InvalidSyntaxException{
		return OutBuilder.out(this, serviceFilter);
	}
	
	/**
	 * Add an {@link ExporterService}
	 * @param factory, the exporter component name.
	 * @return
	 */
	public InstanceBuilder exporter(String factory){
		return InstanceBuilder.instance(this, factory);
	}
	
	/**
	 * Add an {@link ImporterService}
	 * @param factory, The importer component name.
	 * @return
	 */
	public InstanceBuilder importer(String factory){
		return InstanceBuilder.instance(this, factory);
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
		return new ArrayList<InConnection>(ins);
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
		return new ArrayList<OutConnection>(outs);
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
		return new ArrayList<Instance>(instances);
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

    public Map<String,Object> getConf(){
        return new HashMap<String, Object>(conf);
    }

    public int getState(){
        ComponentInstance instance = (ComponentInstance) tracker.getService();
        return (instance == null ? -1 : instance.getState());
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
			host=pHost == null ? host : pHost;
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
            context.ungetService(reference);
		}
		
	}
}
