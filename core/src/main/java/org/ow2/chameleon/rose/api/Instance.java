package org.ow2.chameleon.rose.api;

import static java.util.Collections.singletonMap;
import static org.apache.felix.ipojo.Factory.VALID;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.ow2.chameleon.rose.RoSeConstants.ROSE_REQUIRE_ID;
import static org.ow2.chameleon.rose.RoseMachine.RoSe_MACHINE_ID;

import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.ImporterService;

/**
 * Useful builder that allows for the instantiation of component, such as the
 * ones providing {@link ExporterService}, {@link ImporterService} as well as
 * service discovery and publication facilities.
 * 
 * @author barjo
 * 
 */
public final class Instance {
	private static final String FACTORY_FILTER="("+OBJECTCLASS+"="+Factory.class.getName()+")(factory.state="+VALID+")";
	
	
	private Machine machine;
	private final String component;
	private ServiceTracker tracker;
	private final Hashtable<String, Object> conf = new Hashtable<String, Object>();
	
	private Instance(InstanceBuilder builder) {
		machine = builder.machine;
		component = builder.component;
		conf.putAll(builder.properties);
		try {
			tracker = new ServiceTracker(machine.getContext(), machine.getContext().createFilter("(&"+FACTORY_FILTER+"(factory.name="+component+"))"), new InstanceCreator());
		} catch (InvalidSyntaxException e) {
			throw new IllegalArgumentException("The component name must not contains illegal character.",e);
		}
		machine.add(this);
	}
	
	/**
	 * Start
	 */
	public void start(){
		tracker.open();
	}
	
	/**
	 * Stop
	 */
	public void stop(){
		tracker.close();
    }

    public void update(Machine machine){
        try {
            tracker = new ServiceTracker(machine.getContext(), machine.getContext().createFilter("(&"+FACTORY_FILTER+"(factory.name="+component+"))"), new InstanceCreator());
            this.machine = machine;
            machine.add(this);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("The component name must not contains illegal character.",e);
        }
    }
	
	public static class InstanceBuilder {
		private final Machine machine;
		private final String component;
		
		//Optional
		private Hashtable<String, Object> properties = new Hashtable<String, Object>();
		
		private InstanceBuilder(Machine pMachine, String pComponent) {
			machine = pMachine;
			component = pComponent;
			
			//Add a filter on the RoSeMachine requirement.
			properties.put("requires.filters", 
					new Hashtable<String,Object>(singletonMap(ROSE_REQUIRE_ID, 
				    "("+RoSe_MACHINE_ID+"="+machine.getId()+")")));
			
		}
		
		public static InstanceBuilder instance(Machine pMachine, String pComponent){
			return new InstanceBuilder(pMachine, pComponent);
		}
		
		public InstanceBuilder name(String name ){
			properties.put("instance.name", name);
			return this;
		}
		
		public InstanceBuilder withProperties(Map<String, Object> props){
			properties.putAll(props);
			return this;
		}
		
		public InstanceBuilder withProperty(String key, Object value){
			properties.put(key, value);
			return this;
		}
		
		public Instance create(){
			return new Instance(this);
		}
	}
	
	/**
	 * Get the RoSe_machine component factory and create an instance if binded.
	 * @author barjo
	 */
	private class InstanceCreator implements ServiceTrackerCustomizer {
		
		public Object addingService(ServiceReference reference) {
			Factory factory = (Factory) machine.getContext().getService(reference);
			machine.getContext().ungetService(reference);
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
