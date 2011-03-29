package org.ow2.chameleon.rose.introspect;

import java.util.Collection;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;

/**
 * This service provides minimal introspection about the
 * {@link EndpointDescription} created by the local gateway which are published
 * through the component providing this service.
 * 
 * @author barjo
 */
public interface AnnouncerIntrospection {

	Collection<ExportReference> getAllExportReference();

	Collection<EndpointDescription> getAllEndpointDescription();
	
}
