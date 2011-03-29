package org.ow2.chameleon.rose.introspect;

import java.util.Collection;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;

public interface EndpointCreatorIntrospection {

	/**
	 * @return All {@link ExportReference} of services exported through this service. 
	 */
	Collection<ExportReference> getAllExportReference();
	
	/**
	 * @param sref The {@link ServiceReference}
	 * @return Return the {@link ExportReference} linked to {@code sref} 
	 *  or {@code null} if the service does not have an endpoint created through the service.
	 */
	ExportReference getExportReference(ServiceReference sref);
	
}
