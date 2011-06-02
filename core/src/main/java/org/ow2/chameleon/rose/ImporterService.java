package org.ow2.chameleon.rose;

import java.util.Collection;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;

/**
 * The component providing this service are capable of creating a proxy thanks
 * to an {@link EndpointDescription}.
 * 
 * @author barjo
 */
public interface ImporterService {

	String ENDPOINT_CONFIG_PREFIX = "rose.importer.configs";

	/**
	 * Reify the endpoint of given description as a local service.
	 * 
	 * @param description The {@link EndpointDescription} of the endpoint to be imported.
	 * @return An {@link ImportRegistration}.
	 */
	ImportRegistration importService(EndpointDescription description);
	
	/**
     * @return All {@link ImportReference} of services imported through this
     *         service.
     */
    Collection<ImportReference> getAllImportReference();
	
}
