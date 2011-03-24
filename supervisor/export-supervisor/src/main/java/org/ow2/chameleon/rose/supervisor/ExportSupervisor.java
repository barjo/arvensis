package org.ow2.chameleon.rose.supervisor;

import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.service.log.LogService.LOG_ERROR;
import static org.osgi.service.log.LogService.LOG_WARNING;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
 * TODO complete
 */
public class ExportSupervisor implements ServiceTrackerCustomizer{

	private LogService logger; //The log service
	private ExporterService exporter; //The exporter service
	private ServiceTracker tracker; //use to track the service to be exported
	private BundleContext context; //BundleContext
	private volatile boolean valid = false; 
	private ReadWriteLock rwlock = new ReentrantReadWriteLock();
	
	/**
	 * 
	 * @param pContext
	 */
	public ExportSupervisor(BundleContext pContext) {
		context=pContext;
	}
	
	/**
	 * 
	 */
	private void start(){
		rwlock.writeLock().lock();
		try{
			//Start the tracker if the component has been stopped
			if (tracker != null) {
				tracker.open(); //start the tracker
			}
		
			valid=true;
		}finally{
			rwlock.writeLock().unlock();
		}
	}

	/**
	 * 
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
	 * Called by iPOJO.
	 * @param pExporter
	 */
	@SuppressWarnings("unused")
	private void bindExporterService(ExporterService pExporter){
		exporter = pExporter;
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
	
	/*----------------------------*
	 *  TrackerCustomizer method  *
	 *----------------------------*/
	
	public Object addingService(ServiceReference reference) {
		ExportRegistration registration = null;
		rwlock.readLock().lock();
		try {
			if (valid){
				registration = exporter.exportService(
						reference, null);

				if (registration.getException() == null) { // An exception
															// occurred
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

	public void modifiedService(ServiceReference reference, Object service) {
		//XXX not supported for now
	}

	public void removedService(ServiceReference reference, Object service) {
		//Close the registration
		((ExportRegistration) service).close();
	}
}

