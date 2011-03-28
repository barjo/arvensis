package org.ow2.chameleon.rose;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.ow2.chameleon.rose.introspect.EndpointCreatorIntrospection;

/**
 * The {@link ExporterService} service are provided by the endpoint-creator component.
 * It allows for supervisor component to create the endpoint thanks to the {@link ServiceReference}.
 * For each request to export a service, an {@link ExportRegistration} is returned.
 * 
 * @author barjo
 * @version 0.2.0
 **/
public interface ExporterService extends EndpointCreatorIntrospection{
	
	/**
	 * Create an endpoint for the service of reference {@code sref}.
	 * 
	 * @param sref The {@link ServiceReference} of the service which must be exported
	 * @param properties Additional properties provided by the framework rather than the one provided by {@code sref}
	 * @return An {@link ExportRegistration}.
	 */
	ExportRegistration exportService(ServiceReference sref, Map<String, Object> properties);

}
 
