package org.ow2.chameleon.rose.supervisor;

import static java.lang.String.valueOf;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.service.log.LogService.LOG_ERROR;
import static org.osgi.service.log.LogService.LOG_WARNING;

import java.util.Collection;
import java.util.HashSet;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.handlers.dependency.Dependency;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.introspect.ExportSupervisorIntrospection;

/**
 * Implementation of an <code>export-supervisor</code> {@link ComponentFactory}.
 * Supervise the service export. Track the {@link ServiceReference} of the service which should be exported.
 * This implementation is based on an OSGi {@link Filter} in order to select the service to be exported.
 * The filter is given as a {@link String} through the <code>export.filter</code> property of the component.
 * 
 * A specific {@link ExporterService} could be selected via configuring the <code>exporter-service</code> {@link Dependency}.
 */
public class ExportSupervisor implements ServiceTrackerCustomizer,ExportSupervisorIntrospection{

	private LogService logger; //The log service
	private ExporterService exporter; //The exporter service
	private ServiceTracker tracker; //use to track the service to be exported
	private BundleContext context; //BundleContext, set in the constructor

	/**
	 * <code>true</code> if the instance is in the
	 * {@link ComponentInstance#VALID valid} state, <code>false</code>
	 * otherwise.
	 */
	private volatile boolean valid = false;

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
	@SuppressWarnings("unused")
	private synchronized void start(){
		// Start the tracker
		valid = true; // we are now valid !
		if (tracker != null) {
			tracker.open(); // start the tracker
		}
	}

	/**
	 * This callback is called while the instance is stopping.
	 * It close all {@link ExportRegistration} and the {@link ServiceTracker} which track the service to be exported.
	 */
	@SuppressWarnings("unused")
	private synchronized void stop(){
		valid = false;

		if (tracker != null) {
			tracker.close(); // stop the tracker
		}
	}

	/**
	 * Initialize the service tracker with the <code>export.filter</code> property.
	 * @param filter The export.filter property
	 * @throws InvalidSyntaxException if the <code>export.filter</code> property is not a valid ldap expression.
	 */
	@SuppressWarnings("unused")
	private synchronized void setExportFilter(String filter) {
		try {
			if (valid) {
				tracker.close();
			}

			Filter filterobj = context.createFilter(filter);
			tracker = new ServiceTracker(context, filterobj, this);

			if (valid) {
				tracker.open();
			}
		} catch (InvalidSyntaxException e) {
			logger.log(LOG_ERROR, "Cannot change the export.filter.", e);
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
		try {
			if (valid){
				registration = exporter.exportService(reference, null);
				if (registration.getException() != null) { // exception while endpoint creation, WARNING
					logger.log(LOG_WARNING,
							"An exception occured while creating and endpoint for the service of id: "+ valueOf(reference.getProperty(SERVICE_ID)),
							registration.getException());
				}		
			}
			
		} catch(Exception e){ //defensive catch, e.g registration == null
			logger.log(LOG_ERROR,"Cannot export the service of id: "+valueOf(reference.getProperty(SERVICE_ID)+", the ExporterService failed"),e);
		}
		
		return registration;
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
	
	/*-------------------------*
	 * Introspection Service   *
	 *-------------------------*/

	public Collection<ExportReference> getAllExportReference() {
		Collection<ExportReference> exrefs = new HashSet<ExportReference>();
		Object[] registrations = (Object[]) tracker.getServices();

		if (registrations == null){
			return exrefs;
		}
		
		for (Object exReg : registrations) {
			ExportReference ref = ((ExportRegistration) exReg).getExportReference();
			if(ref != null) exrefs.add(ref);
		}
		
		return exrefs;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.introspect.ExportSupervisorService#getExportReference(org.osgi.framework.ServiceReference)
	 */
	public ExportReference getExportReference(ServiceReference sref) {
		ExportRegistration reg = (ExportRegistration) tracker.getService(sref);
		
		if (reg == null){
			return null;
		}
		
		return reg.getExportReference();
	}
}

