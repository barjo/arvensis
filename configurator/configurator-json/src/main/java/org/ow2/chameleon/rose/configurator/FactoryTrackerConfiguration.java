package org.ow2.chameleon.rose.configurator;

import static java.util.Collections.singletonMap;
import static org.apache.felix.ipojo.Factory.VALID;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.FrameworkUtil.createFilter;
import static org.ow2.chameleon.rose.RoseMachine.ROSE_MACHINE_ID;

import java.util.Hashtable;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class FactoryTrackerConfiguration implements RoseConfiguration,ServiceTrackerCustomizer {
	private static final String FACTORY_FILTER="("+OBJECTCLASS+"="+Factory.class.getName()+")(factory.state="+VALID+")";

	private static final Object ROSE_DEP_ID = "rose.machine";
	
	private final ServiceTracker tracker;
	private final BundleContext context;
	private final Hashtable<String, Object> dico;
	
	public FactoryTrackerConfiguration(BundleContext pContext, String component,
			Hashtable<String, Object> properties, String machineId) throws InvalidSyntaxException {
		context=pContext;
		
		dico=properties;
		
		if (machineId != null){
			dico.put("requires.filters", 
					new Hashtable(singletonMap(ROSE_DEP_ID, 
							    "("+ROSE_MACHINE_ID+"="+machineId+")")));
			dico.put("instance.name", component+"-"+machineId);
		}
		
		StringBuilder sb = new StringBuilder("(&");
		sb.append(FACTORY_FILTER);
		sb.append("(factory.name=");
		sb.append(component);
		sb.append("))");
		
		tracker = new ServiceTracker(context,createFilter(sb.toString()),this);
	}

	@Override
	public void start() {
		tracker.open();

	}

	@Override
	public void stop() {
		try{
			tracker.close();
		}catch(Exception e){
			//TODO LOG
		}
	}


	@Override
	public Object addingService(ServiceReference reference) {
		Factory factory = (Factory) context.getService(reference);
		try {
			return factory.createComponentInstance(dico);
		} catch (UnacceptableConfiguration e) {
			e.printStackTrace();
		} catch (MissingHandlerException e) {
			e.printStackTrace();
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public void modifiedService(ServiceReference reference, Object service) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		((ComponentInstance) service).dispose();
		context.ungetService(reference);
	}

}
