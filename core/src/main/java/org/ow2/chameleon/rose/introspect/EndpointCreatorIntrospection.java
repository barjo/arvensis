package org.ow2.chameleon.rose.introspect;

import java.util.Collection;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;

/**
 * This service provides minimal introspection about the service created by the
 * <code>endpoint-creator</core> component providing this service.
 * 
 * @author barjo
 * @version 0.2.0
 */
public interface EndpointCreatorIntrospection {
	/**
	 * The name of the property related to {@link EndpointCreatorIntrospection#getConfigPrefix()}.
	 */
	String ENDPOINT_CONFIG_PREFIX = "rose.endpoint-creator.configs";

	/**
	 * @return All {@link ExportReference} of services exported through this
	 *         component instance.
	 */
	Collection<ExportReference> getAllExportReference();

	/**
	 * @param sref The {@link ServiceReference} of an exported service.
	 * @return Return the {@link ExportReference} linked to {@code sref} or
	 *         {@code null} if the service does not have an endpoint created
	 *         through this component.
	 */
	ExportReference getExportReference(ServiceReference sref);
	
	/**
	 * @return The configuration prefix used or defined by the endpoint-creator. (i.e <code>json-rpc,org.jabsorb,jax-rs</code>.
	 */
	List<String> getConfigPrefix();

}
