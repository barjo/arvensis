package org.ow2.chameleon.rose.registry;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Service provided by the RRegistry component. Such component allows to
 * discover {@link EndpointDescription} provided by remote components as well as
 * to register {@link EndpointDescription} of endpoint available from the local
 * gateway.
 * 
 * @author barjo
 */
public interface RRegistryService extends RRegistryListeningService,
		RRegistryRegistrationService {

}
