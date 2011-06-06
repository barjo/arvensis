package org.ow2.chameleon.rose.internal;

import static org.osgi.service.remoteserviceadmin.EndpointListener.ENDPOINT_LISTENER_SCOPE;
import static org.ow2.chameleon.rose.util.RoseTools.getEndpointListenerInterrest;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.rose.RoseMachine.EndpointListerInterrest;
import org.ow2.chameleon.rose.registry.ExportRegistry;
import org.ow2.chameleon.rose.registry.ImportRegistry;

public class EndpointListenerTracker implements ServiceTrackerCustomizer {
	private final ServiceTracker tracker; 
	private final ImportRegistry importReg;
	private final ExportRegistry exportReg;
	private final BundleContext context;
	
	
	public EndpointListenerTracker(BundleContext pContext,ImportRegistry impReg, ExportRegistry expReg) {
		context = pContext;
		importReg =  impReg;
		exportReg = expReg;
		
		tracker = new ServiceTracker(context, EndpointListener.class.getName(), this);
	}
	
	public void open(){
		tracker.open();
	}
	
	public void close(){
		tracker.close();
	}
	

	public Object addingService(ServiceReference reference) {
		EndpointListener listener = (EndpointListener) context.getService(reference);
		Object ofilter = reference.getProperty(ENDPOINT_LISTENER_SCOPE);
		EndpointListerInterrest interrest = getEndpointListenerInterrest(reference);
		

		try{
			switch (interrest) {
			case ALL:
				exportReg.addEndpointListener(listener, (String) ofilter);
				importReg.addEndpointListener(listener, (String) ofilter);
				break;
			case REMOTE:
				exportReg.addEndpointListener(listener, (String) ofilter);
				break;
			case LOCAL:
				exportReg.addEndpointListener(listener, (String) ofilter);
			}
		}catch (Exception e) {
//			logger.log(LOG_ERROR, 
//					"Cannot add the EndpointLister of service id: " +
//					reference.getProperty(SERVICE_ID),e);
			
			//Cautious remove
			importReg.removeEndpointListener(listener);
			exportReg.removeEndpointListener(listener);
			
			listener = null; //Do not track
		}
		
		return new InterestedListener(listener,interrest);
	}

	public void modifiedService(ServiceReference reference, Object service) {
		InterestedListener listener = (InterestedListener) service;
		Object ofilter = reference.getProperty(ENDPOINT_LISTENER_SCOPE); //new filter

		try {
			switch (listener.getInterest()) {
			case ALL:
				exportReg.addEndpointListener(listener.getListener(),
						(String) ofilter);
				importReg.addEndpointListener(listener.getListener(),
						(String) ofilter);
				break;
			case REMOTE:
				exportReg.addEndpointListener(listener.getListener(),
						(String) ofilter);
				break;
			case LOCAL:
				exportReg.addEndpointListener(listener.getListener(),
						(String) ofilter);
			}
		} catch (Exception e) {
//			logger.log(LOG_ERROR,
//					"Cannot update the EndpointListner of service id: "
//							+ reference.getProperty(SERVICE_ID), e);

			// Cautious remove
			importReg.removeEndpointListener(listener.getListener());
			exportReg.removeEndpointListener(listener.getListener());
		}
		
	}

	public void removedService(ServiceReference reference, Object service) {
		InterestedListener listener = (InterestedListener) service;

		switch (listener.getInterest()) {
		case ALL:
			importReg.removeEndpointListener(listener.getListener());
			exportReg.removeEndpointListener(listener.getListener());
			break;
		case REMOTE:
			exportReg.removeEndpointListener(listener.getListener());
			break;
		case LOCAL:
			importReg.removeEndpointListener(listener.getListener());
		}

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