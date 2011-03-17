package org.ow2.chameleon.rose;

import java.util.Collection;
import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

/**
 * The ExporterService service are provided by the endpoint-creator component.
 * It allows for supervisor component to create the endpoint thanks to the ServiceReference.
 * For each request to export a service, an ExportRegistration is returned.
 * 
 * @author barjo
 **/
public interface ExporterService {
	
	/**
	 * Create an endpoint for the service of given ServiceReference.
	 * 
	 * @param sref The ServiceReference of the service which must be exported.
	 * @param properties Additional properties provided by the framework rather that the service itself.
	 * @return An ExportRegistration
	 */
	ExportRegistration exportService(ServiceReference sref, Map<String, Object> properties);
	
	/**
	 * @return All ExportReference of services exported through this service. 
	 */
	Collection<ExportReference> getAllExportReference();
	
	/**
	 * @param sref
	 * @return Return the ExportReference linked to the given ServiceReference 
	 *  or null if the service does not have an endpoint created through the service.
	 */
	ExportReference getExportReference(ServiceReference sref);
}
 
