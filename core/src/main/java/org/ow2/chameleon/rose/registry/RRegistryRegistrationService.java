package org.ow2.chameleon.rose.registry;

import org.osgi.service.remoteserviceadmin.ExportReference;

/**
 * This service allows to register a newly available endpoint within an RRegistry.
 * @author barjo
 */
public interface RRegistryRegistrationService {
	
	/**
	 * Register a newly available endpoint within the RRegistry.
	 * @param xref The {@link ExportReference} of the endpoint.
	 * @return The {@link RRegistration} object which allows for the unregistration (i.e. unpublication) of an endpoint previously registered.
	 */
	RRegistration registerEndpoint(ExportReference xref);
}

