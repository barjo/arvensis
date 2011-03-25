package org.ow2.chameleon.rose.supervisor;

import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.service.log.LogService.LOG_ERROR;
import static org.osgi.service.log.LogService.LOG_WARNING;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.handlers.dependency.Dependency;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.rose.ExporterService;

/**
 * Implementation of an <code>export-supervisor</code> {@link ComponentFactory}.
 * Supervise the service export. Track the {@link ServiceReference} of the service which should be exported.
 * This implementation is based on an OSGi {@link Filter} in order to select the service to be exported.
 * The filter is given as a {@link String} through the <code>export.filter</code> property of the component.
 * 
 * A specific {@link ExporterService} could be selected via configuring the <code>exporter-service</code> {@link Dependency}.
 */
public class ExportSupervisor implements ServiceTrackerCustomizer{

	private LogService logger; //The log service
	private ExporterService exporter; //The exporter service
	private ServiceTracker tracker; //use to track the service to be exported
	private BundleContext context; //BundleContext, set in the constructor

	/**
	 * <code>true</code> if the instance is in the
	 * {@link ComponentInstance#VALID valid} state, <code>false</code>
	 * otherwise.
	 */
	private boolean valid = false;

	/**
	 * {@link ReadWriteLock}, write {@link Lock lock} while stopping and
	 * starting, read {@link Lock lock} while exporting a service/
	 */
	private ReadWriteLock rwlock = new ReentrantReadWriteLock();
	
	/**
	 * Constructor, the {@link BundleContext} is injected by iPOJO.
	 * @param pContext
	 */
	public ExportSupervisor(BundleContext pContext) {
		context=pContext;
	}
	
	/**
	 * This callback is call while the instance is starting.
	 * It starts the {@link ServiceTracker} which track the service to be exported.
	 */
	private void start(){
		rwlock.writeLock().lock();
		try{
			//Start the tracker if the component has been stopped
			//TODO close all ExportRegistration thanks to tracker.getObjects() ????
			if (tracker != null) {
				tracker.open(); //start the tracker
			}
		
			valid=true; //we are now valid !
		}finally{
			rwlock.writeLock().unlock();
		}
	}

	/**
	 * This callback is called while the instance is stopping.
	 * It close all {@link ExportRegistration} and the {@link ServiceTracker} which track the service to be exported.
	 */
	private void stop(){
		rwlock.writeLock().lock();
		try{
			valid = false;

			if (tracker != null) {
				tracker.close(); // stop the tracker
			}
		}finally{
			rwlock.writeLock().unlock();
		}
	}

	/**
	 * Initialize the service tracker with the <code>export.filter</code> property.
	 * @param filter The export.filter property
	 * @throws InvalidSyntaxException if the <code>export.filter</code> property is not a valid ldap expression.
	 */
	@SuppressWarnings("unused")
	private void setExportFilter(String filter) {
		//XXX is this method called while the instance is in a valid state ?
		stop();
		try {
			Filter filterobj = context.createFilter(filter);
			tracker = new ServiceTracker(context, filterobj, this);
			start();
		} catch (InvalidSyntaxException e) {
			logger.log(LOG_ERROR,
					"Cannot start the export supervisor component.", e);
		}
	}
	
	/*-----------------------------*
	 *  TrackerCustomizer methods  *
	 *-----------------------------*/
	

	/**
	 * A service required to be exported has been added. 
	 * Creatre an endpoint thanks to the {@link ExporterService}.
	 * {@link ServiceTrackerCustomizer#addingService(ServiceReference)}
	 */
	public Object addingService(ServiceReference reference) {
		ExportRegistration registration = null;
		rwlock.readLock().lock();
		try {
			if (valid){
				registration = exporter.exportService(
						reference, null);

				if (registration.getException() == null) { // cannot export
					logger.log(
							LOG_WARNING,
							"Cannot export the service of id: "
									+ String.valueOf(reference
											.getProperty(SERVICE_ID)),
							registration.getException());
					registration.close();
				}
				// XXX track even if the registration has thrown an exception ?
			}
			return registration;
		} finally {
			rwlock.readLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	public void modifiedService(ServiceReference reference, Object service) {
		//XXX not supported for now
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	public void removedService(ServiceReference reference, Object service) {
		//Close the registration
		((ExportRegistration) service).close();
	}
}

