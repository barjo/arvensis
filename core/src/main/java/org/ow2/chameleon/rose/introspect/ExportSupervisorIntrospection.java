package org.ow2.chameleon.rose.introspect;

import java.util.Collection;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;

/**
 * This service allows minimal introspection on the supervisor component. 
 * @author barjo
 */
public interface ExportSupervisorIntrospection {

	ExportReference getExportReference(ServiceReference sref);
	
	Collection<ExportReference> getAllExportReference();
	
}
