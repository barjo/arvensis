package org.ow2.chameleon.rose;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The {@link ExporterService} service are provided by the exporter component.
 * It allows for supervisor component to create the endpoint thanks to the {@link ServiceReference}.
 * For each request to export a service, an {@link ExportRegistration} is returned.
 * 
 * @author barjo
 * @version 1.0.1
 **/
public interface ExporterService {

	/**
	 * @return The configuration prefix used or defined by the exporter. (i.e <code>json-rpc,org.jabsorb,jax-rs</code>.
	 */
	List<String> getConfigPrefix();

	/**
	 * Create an endpoint for the service of reference {@code sref}.
	 * 
	 * @param sref The {@link ServiceReference} of the service which must be exported
	 * @param properties Additional properties provided by the framework rather than the one provided by {@code sref}
	 * @return An {@link ExportRegistration}.
	 */
	ExportRegistration exportService(ServiceReference sref, Map<String, ?> properties);

	/**
     * @return All {@link ExportReference} of services exported through this
     *         service.
     */
    Collection<ExportReference> getAllExportReference();


    /**
     * @return The {@code RoseMachine} linked to this {@code ExporterService}
     */
    RoseMachine getRoseMachine();
}
 
