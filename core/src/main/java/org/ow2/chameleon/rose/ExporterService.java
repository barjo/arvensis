package org.ow2.chameleon.rose;

import java.util.Collection;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

/**
 * The ExporterService service are provided by the endpoint-creator component.
 * It allows for supervisor component to create the endpoint thanks to the ServiceReference.   
 **/
public interface ExporterService {
	ExportRegistration exportService(ServiceReference sref);
	Collection<ExportReference> getAllExportReference();
}
 
