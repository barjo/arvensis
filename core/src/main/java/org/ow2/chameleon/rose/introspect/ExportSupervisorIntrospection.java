package org.ow2.chameleon.rose.introspect;

import java.util.Collection;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;

/**
 * This service allows minimal introspection on the
 * </code>export-supervisor</code> component.
 * 
 * @author barjo
 */
public interface ExportSupervisorIntrospection {

	/**
	 * @param sref
	 *            The tracked {@link ServiceReference}.
	 * @return the {@link ExportReference} related to <code>sref</code> or null
	 *         is the export of the service of reference <code>sref</code> is
	 *         not supervised by <code>export-supervisor</code> providing this service.
	 */
	ExportReference getExportReference(ServiceReference sref);

	/**
	 * @return The collection of All {@link ExportReference} supervised by the
	 *         <code>export-supervisor</code> providing this service.
	 */
	Collection<ExportReference> getAllExportReference();

}
