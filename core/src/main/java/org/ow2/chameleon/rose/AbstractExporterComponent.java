package org.ow2.chameleon.rose;

import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.ow2.chameleon.rose.util.RoseTools.computeEndpointExtraProperties;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.internal.BadExportRegistration;
import org.ow2.chameleon.rose.registry.ExportRegistryProvisioning;
import org.ow2.chameleon.rose.util.ConcurrentMapOfSet;

/**
 * Abstract implementation of an endpoint-creator {@link ComponentFactory} which provides an {@link ExporterService}.
 * 
 * @version 0.2.0
 * @author barjo
 */
public abstract class AbstractExporterComponent implements ExporterService {
	private final ConcurrentMapOfSet<ServiceReference, MyExportRegistration> registrations;
	private volatile boolean isValid = false;
	
	
	public  AbstractExporterComponent() {
		registrations = new ConcurrentMapOfSet<ServiceReference, MyExportRegistration>();
	}
	
	/*--------------------------*
	 * EndpointCreator methods. *
	 *--------------------------*/
	
	/**
	 * Create an endpoint for the service linked to {@code sref}. 
	 * If the service has already an endpoint with compatible properties, 
	 * return the existing {@link EndpointDescription}, otherwise an exception must be thrown.
	 * @param sref The {@link ServiceReference}
	 * @return The {@link EndpointDescription} of the created endpoint.
	 */
	protected abstract EndpointDescription createEndpoint(ServiceReference sref,Map<String,Object> extraProperties);
	
	/**
	 * Destroy the endpoint of {@code endesc} description.
	 * @param endesc The {@link EndpointDescription}
	 */
	protected abstract void destroyEndpoint(EndpointDescription endesc);
	
	/**
	 * Stop the endpoint-creator, iPOJO Invalidate instance callback.
	 * Must be override !
	 * Close all endpoints.
	 */
	 protected void stop(){
		
		synchronized (registrations) {
			Collection<ServiceReference> srefs = registrations.keySet();
		
			for (Iterator<ServiceReference> iterator = srefs.iterator(); iterator.hasNext();) {
				ServiceReference ref = iterator.next();
				ExportReference xref = registrations.getElem(ref).getExportReference();
				getExportRegistry().remove(xref); //TODO check !=null
				destroyEndpoint(xref.getExportedEndpoint()); //TODO check != null
				iterator.remove();
			}
			
			isValid = false;
		}
	 }
	 
	 /**
	  * Start the endpoint-creator component, iPOJO Validate instance callback.
	  * Must be override !
	  */
	 protected void start(){
		 synchronized (registrations) {
			isValid = true;
			
			if (registrations.size() > 0){
				getLogService().log(LOG_WARNING, "Internal structures have not been cleared while stopping the instance.");
			}
		 }
	 }
	 
	 /**
	  * @return <code>true</code> if the {@link ComponentInstance} is in a valid state, <code>false</code> otherwise.
	  */
	 protected final boolean isValid(){
			return isValid;
	 }
	
	/*--------------------------------------------*
	 *  Convenient access to some useful service. *
	 *--------------------------------------------*/
	
	/**
	 * @return The {@link LogService} service.
	 */
	protected abstract LogService getLogService();
	
	/**
	 * @return The {@link EventAdmin} service.
	 */
	protected abstract EventAdmin getEventAdmin();
	
	/**
	 * @return The {@link ExportRegistryProvisioning} service.
	 */
	protected abstract ExportRegistryProvisioning getExportRegistry();
	
	/*---------------------------------*
	 *  ExporterService implementation *
	 *---------------------------------*/
	
	/**
	 * @param sref
	 * @param extraProperties
	 * @return
	 */
	public final ExportRegistration exportService(ServiceReference sref,Map<String,Object> extraProperties) {
		final ExportRegistration xreg;
		
		synchronized (registrations) {
			
			if (!isValid){
				return new BadExportRegistration(new Throwable("This ExporterService is no more available. !"));
			}
			
			if (registrations.containsKey(sref)) { 
				//Clone Registration
				xreg = new MyExportRegistration(registrations.getElem(sref));
			} else { 
				//First registration, create the endpoint
				EndpointDescription enddesc = createEndpoint(sref, computeEndpointExtraProperties(sref, extraProperties, getConfigPrefix()));
				
				xreg = new MyExportRegistration(sref,enddesc);
			}
		}
		
		return xreg;
	}

	
	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.ExporterService#getExportReference(org.osgi.framework.ServiceReference)
	 */
	public ExportReference getExportReference(ServiceReference sref) {
		
		if(!registrations.containsKey(sref)){
			return null;
		}
		return registrations.getElem(sref).getExportReference();
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.introspect.ExporterIntrospection#getAllExportReference()
	 */
	public Collection<ExportReference> getAllExportReference(){
		Collection<ExportReference> xrefs = new HashSet<ExportReference>();
		
		for (ServiceReference sref : registrations.keySet()) {
			xrefs.add(registrations.getElem(sref).getExportReference());
		}
		
		return xrefs;
	}
	
	
	/*--------------------------------*
	 *         INNER CLASS            *
	 *--------------------------------*/
	
	/**
	 * Implementation of an {@link ExportReference}.
	 * 
	 * @author barjo
	 */
	private final class MyExportReference implements ExportReference {
		private ServiceReference sref;
		private EndpointDescription desc;
		
		/**
		 * Register the {@code pEndesc} thanks to {@code context}.
		 * 
		 * @param pSref
		 * @param pEnddesc
		 * @param context
		 */
		public MyExportReference(ServiceReference pSref,EndpointDescription pEnddesc) {
			sref = pSref;
			desc = pEnddesc;
		}

		/*
		 * (non-Javadoc)
		 * @see org.osgi.service.remoteserviceadmin.ExportReference#getExportedService()
		 */
		public ServiceReference getExportedService() {
				return sref;
		}

		/*
		 * (non-Javadoc)
		 * @see org.osgi.service.remoteserviceadmin.ExportReference#getExportedEndpoint()
		 */
		public EndpointDescription getExportedEndpoint() {
				return desc;
		}
	}
	
	/**
	 * Implementation of an {@link ExportRegistration}.
	 * 
	 * @author barjo
	 */
	private final class MyExportRegistration implements ExportRegistration {
		private volatile ExportReference xref;
		
		private MyExportRegistration(MyExportRegistration reg) {
			xref=reg.getExportReference();
			
			//Add the registration to the registrations mapOfSet
			registrations.add(reg.getExportReference().getExportedService(), this);
		}
		
		private MyExportRegistration(ServiceReference sref, EndpointDescription desc) {
			//Create the ExportedReference
			xref = new MyExportReference(sref, desc); 
			
			//Add the registration to the registrations mapOfSet
			registrations.add(xref.getExportedService(), this);
			
			//register the ExportReference within the ExportRegistry
			getExportRegistry().put(xref,xref);
		}
		
		
		/*
		 * (non-Javadoc)
		 * @see org.osgi.service.remoteserviceadmin.ExportRegistration#getExportReference()
		 */
		public ExportReference getExportReference() {
			return xref;
		}

		/*
		 * (non-Javadoc)
		 * @see org.osgi.service.remoteserviceadmin.ExportRegistration#close()
		 */
		public void close() {
			if (xref != null) {
				// Last registration, remove the ExportReference from the ExportRegistry
				if (registrations.remove(xref.getExportedService(), this)) {
					getExportRegistry().remove(xref);
				}
				xref = null; // is now closed
			}
		}

		/*
		 * (non-Javadoc)
		 * @see org.osgi.service.remoteserviceadmin.ExportRegistration#getException()
		 */
		public Throwable getException() {
			return null;
		}
	}
	
}
