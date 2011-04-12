package org.ow2.chameleon.rose;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ExportReference;

/**
 * Service provided by the rose-registry component. Such component allows to
 * discover {@link EndpointDescription} provided by remote components.
 * 
 * @author barjo
 */
public interface RoSeRegistryService {

	RoSeRegistration registerExportedService(ExportReference xref);

	void addEndpointListener(EndpointListener listener);

	void addEndpointListener(EndpointListener listener, String filter);

	void removeEndpointListener(EndpointListener listener);
}
