package org.ow2.chameleon.rose.introspect;

import java.util.Collection;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;

/**
 * This service provides minimal introspection about the
 * {@link EndpointDescription} created by the local gateway which are published
 * through the <code>announcer</code> providing this service.
 * 
 * @author barjo
 */
public interface AnnouncerIntrospection {

	/**
	 * @return The collection of All {@link ExportReference} tracked by the
	 *         <code>announcer</code> providing this service.
	 */
	Collection<ExportReference> getAllExportReference();

	/**
	 * @return The collection of All {@link EndpointDescription} published by
	 *         the <code>announcer</code> providing this service.
	 */
	Collection<EndpointDescription> getAllEndpointDescription();

}
