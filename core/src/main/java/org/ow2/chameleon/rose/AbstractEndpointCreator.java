package org.ow2.chameleon.rose;

import static org.osgi.service.log.LogService.LOG_WARNING;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.internal.BadExportRegistration;
import org.ow2.chameleon.rose.util.ConcurrentMapOfSet;

/**
 * Abstract implementation of an endpoint-creator {@link ComponentFactory} which provides an {@link ExporterService}.
 * 
 * @version 0.2.0
 * @author barjo
 */
public abstract class AbstractEndpointCreator implements ExporterService {
	private final Map<ServiceReference, MyExportReference> references;
	private final ConcurrentMapOfSet<MyExportReference, MyExportRegistration> registrations;
	private volatile boolean isValid = false;
	
	private final BundleContext context;
	
	public  AbstractEndpointCreator(BundleContext pContext) {
		context=pContext;
		references = new HashMap<ServiceReference, MyExportReference>();
		registrations = new ConcurrentMapOfSet<MyExportReference, MyExportRegistration>();
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
	 * Close all endpoints.
	 */
	 protected void stop(){
		registrations.clear();
		
		synchronized (references) {
			Collection<MyExportReference> endrefs = references.values();
		
			for (Iterator<MyExportReference> iterator = endrefs.iterator(); iterator.hasNext();) {
				MyExportReference myref = iterator.next();
				myref.close();
			}
			
			references.clear();
			isValid = false;
		}
	 }
	 
	 protected void start(){
		 synchronized (references) {
			isValid = true;
			
			if (references.size() > 0 || registrations.size() > 0){
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
	
	/*---------------------------------*
	 *  ExporterService implementation *
	 *---------------------------------*/
	
	/**
	 * FIXME
	 * @param sref
	 * @param extraProperties
	 * @return
	 */
	
	public final ExportRegistration exportService(ServiceReference sref,Map<String,Object> extraProperties) {
		final ExportRegistration xreg;
		
		synchronized (references) {
			
			if (!isValid){
				return new BadExportRegistration(new Throwable("This ExporterService is no more available. !"));
			}
			
			if (references.containsKey(sref)) {
				xreg = new MyExportRegistration(references.get(sref), null);
			} else {
				EndpointDescription enddesc = createEndpoint(sref,
						extraProperties);
				MyExportReference xref = new MyExportReference(sref, enddesc,
						context);
				xreg = new MyExportRegistration(xref, null);
			}
		}
		
		return xreg;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.ExporterService#getAllExportReference()
	 */
	public final Collection<ExportReference> getAllExportReference() {
		return new HashSet<ExportReference>(registrations.keySet());
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.ExporterService#getExportReference(org.osgi.framework.ServiceReference)
	 */
	public ExportReference getExportReference(ServiceReference sref) {
		return references.get(sref);
	}
	
	
	/*--------------------------------*
	 *         INNER CLASS            *
	 *--------------------------------*/
	
	/**
	 * Implementation of an {@link ExportReference}.
	 * 
	 * When {@link MyExportReference#close()} is called, the endpoint is destroyed and unregistered. 
	 * @author barjo
	 */
	private final class MyExportReference implements ExportReference {
		private ServiceReference sref;
		private EndpointDescription desc;
		private ServiceRegistration regis;
		private volatile boolean closed = false;
		
		/**
		 * Register the {@code pEndesc} thanks to {@code context}.
		 * 
		 * @param pSref
		 * @param pEnddesc
		 * @param context
		 */
		public MyExportReference(ServiceReference pSref,EndpointDescription pEnddesc,BundleContext context) {
			sref = pSref;
			desc = pEnddesc;
			regis = context.registerService(ExportReference.class.getName(),this, toDico(pEnddesc));
			
			//add the export reference to the references.
			references.put(sref, this);
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

		/**
		 * Unregister the {@link EndpointDescription} and destroy the endpoint,
		 * by calling
		 * {@link AbstractEndpointCreator#destroyEndpoint(EndpointDescription)}.
		 */
		public void close() {
			synchronized (references) {
				if (!closed) {
					//remove the reference of the references
					references.remove(sref);
					
					// unregister the endpointDescription and destroy the
					// endpoint
					regis.unregister();
					destroyEndpoint(desc);
					regis = null;
					sref = null;
					desc = null;
					closed = true; //is now closed
				}
			}
		}
	}
	
	/**
	 * Implementation of an {@link ExportRegistration}.
	 * 
	 * @author barjo
	 */
	private final class MyExportRegistration implements ExportRegistration {
		private volatile MyExportReference xref;
		
		private MyExportRegistration(MyExportReference pXref,Throwable pException) {
			xref=pXref;
			//Add the registration to the registrations mapOfSet
			registrations.add(xref, this);
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
				// Last registration, close the ExportReference
				if (registrations.remove(xref, this)) {
					xref.close();
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
	
	/**
	 * Return a Dictionary representation of the EndpointDescription.
	 * @param enddesc
	 * @return a Dictionary representation of the EndpointDescription.
	 */
	private static Dictionary<String, Object> toDico(EndpointDescription enddesc){
		return new Hashtable<String, Object>(enddesc.getProperties());
	}
}
