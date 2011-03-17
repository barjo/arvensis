package org.ow2.chameleon.rose;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.util.ConcurrentMapOfSet;

/**
 * Abstract implementation of an endpoint-creator component, the endpoint-creator must provide an ExporterService.
 * @author barjo
 */
public abstract class AbstractEndpointCreator implements ExporterService {
	protected final Map<ServiceReference, MyExportReference> references;
	protected final ConcurrentMapOfSet<MyExportReference, MyExportRegistration> registrations;
	
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
	 * Create an endpoint for the service linked to the given ServiceReference. 
	 * If the service has already an endpoint with compatible properties, 
	 * return the existing EndpointDescription, otherwise an exception must be thrown.
	 * @param sref
	 * @return The EndpointDescription of the created endpoint.
	 */
	protected abstract EndpointDescription createEndpoint(ServiceReference sref,Map<String,Object> extraProperties);
	
	/**
	 * Destroy the endpoint of given EndpointDescription.
	 * @param endesc
	 */
	protected abstract void destroyEndpoint(EndpointDescription endesc);
	
	/**
	 * Close all endpoints.
	 */
	 protected void stop(){
		
	 }
	
	/*--------------------------------------------*
	 *  Convenient access to some useful service. *
	 *--------------------------------------------*/
	
	/**
	 * @return The LogService.
	 */
	protected abstract LogService getLogService();
	
	/**
	 * @return The EventAdmin service.
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
		
		if (references.containsKey(sref)) {
			xreg = new MyExportRegistration(references.get(sref), null);
		} else {
			EndpointDescription enddesc = createEndpoint(sref,extraProperties);
			MyExportReference xref = new MyExportReference(sref, enddesc,context);
			xreg = new MyExportRegistration(xref, null);
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
	 * Implementation of an ExportReference.
	 * @author barjo
	 */
	private final class MyExportReference implements ExportReference {
		private ServiceReference sref;
		private EndpointDescription desc;
		private ServiceRegistration regis;
		private volatile boolean closed = false;
		private ReadWriteLock rwlock = new ReentrantReadWriteLock();
		
		public MyExportReference(ServiceReference pSref,EndpointDescription pEnddesc,BundleContext context) {
			sref = pSref;
			desc = pEnddesc;
			regis = context.registerService(EndpointDescription.class.getName(),pEnddesc, toDico(pEnddesc));
			
			//add the export reference to the references.
			references.put(sref, this);

		}

		public ServiceReference getExportedService() {
			rwlock.readLock().lock();
			try{
				return sref;
			}finally {
				rwlock.readLock().unlock();
			}
		}

		public EndpointDescription getExportedEndpoint() {
			rwlock.readLock().lock();
			try{
				return desc;
			}finally {
				rwlock.readLock().unlock();
			}
		}
		
		public void close() {
			rwlock.writeLock().lock();
			try {
				if (!closed) {
					//remove the reference of the references
					references.remove(this);
					
					// unregister the endpointDescription and destroy the
					// endpoint
					regis.unregister();
					destroyEndpoint(desc);
					regis = null;
					sref = null;
					desc = null;
					closed = true; //is now closed
				}
			} finally {
				rwlock.writeLock().unlock();
			}
		}
	}
	
	/**
	 * Implementation of an ExportRegistration.
	 * @author barjo
	 */
	private final class MyExportRegistration implements ExportRegistration {
		private MyExportReference xref;
		private Throwable exception;
		
		private volatile boolean closed = false;

		
		private MyExportRegistration(MyExportReference pXref,Throwable pException) {
			xref=pXref;
			exception = pException;
			
			//Add the registration to the registrations mapOfSet
			registrations.add(xref, this);
		}
		
		public synchronized MyExportReference getExportReference() {
			return xref;
		}

		public synchronized void close() {
			if (!closed) {
				
				//Last registration, close the ExportReference
				if (registrations.remove(xref, this)) {
					xref.close();
				}
				xref = null;
				exception = null;
				closed = true; //is now closed
			}
		}

		public synchronized Throwable getException() {
			return exception;
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
