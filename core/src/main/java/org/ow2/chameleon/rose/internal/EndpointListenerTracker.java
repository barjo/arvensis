package org.ow2.chameleon.rose.internal;

import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.osgi.service.remoteserviceadmin.EndpointListener.ENDPOINT_LISTENER_SCOPE;
import static org.ow2.chameleon.rose.util.RoseTools.getEndpointListenerInterrest;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.rose.RoseMachine.EndpointListerInterrest;

public class EndpointListenerTracker implements ServiceTrackerCustomizer {
	private final ServiceTracker tracker; 
	private final RoseMachineImpl machine;
	
	
	public EndpointListenerTracker(RoseMachineImpl pMachine) {
		machine = pMachine;
		
		tracker = new ServiceTracker(machine.getContext(), EndpointListener.class.getName(), this);
	}
	
	public void open(){
		tracker.open();
	}
	
	public void close(){
		tracker.close();
	}
	

	public Object addingService(ServiceReference reference) {
		EndpointListener listener = (EndpointListener) machine.getContext().getService(reference);
		Object ofilter = reference.getProperty(ENDPOINT_LISTENER_SCOPE);
		String sfilter = ofilter == null ? null : String.valueOf(ofilter);
		EndpointListerInterrest interrest = getEndpointListenerInterrest(reference);
		
		try {
			machine.addEndpointListener(listener, interrest, sfilter);
		} catch (InvalidSyntaxException e) {
			machine.log(LOG_WARNING, "cannot add EndpointListener of reference: "+reference,e);
		}
		
		return new InterestedListener(listener,interrest);
	}

	public void modifiedService(ServiceReference reference, Object service) {
		InterestedListener listener = (InterestedListener) service;
		Object ofilter = reference.getProperty(ENDPOINT_LISTENER_SCOPE); //new filter
		String sfilter = ofilter == null ? null : String.valueOf(ofilter);


		try {
			machine.addEndpointListener(listener.getListener(), listener.getInterest(), sfilter);
		} catch (InvalidSyntaxException e) {
			machine.log(LOG_WARNING, "cannot update EndpointListener of reference: "+reference,e);
		}
		
		
	}

	public void removedService(ServiceReference reference, Object service) {
		InterestedListener listener = (InterestedListener) service;
		machine.removeEndpointListener(listener.getListener(), listener.getInterest());
	}
	
	public final class InterestedListener {
		private final EndpointListener listener;
		private EndpointListerInterrest interrest;

		public InterestedListener(EndpointListener pListener,
				EndpointListerInterrest pInterrest) {
			listener = pListener;
			interrest = pInterrest;
		}

		public EndpointListener getListener() {
			return listener;
		}

		public EndpointListerInterrest getInterest() {
			return interrest;
		}
	}
	
}