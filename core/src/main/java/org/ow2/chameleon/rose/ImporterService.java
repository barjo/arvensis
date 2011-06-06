package org.ow2.chameleon.rose;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

	String ENDPOINT_CONFIG_PREFIX = "rose.protos.configs";

	/**
	 * Reify the endpoint of given description as a local service.
	 * 
	 * @param description The {@link EndpointDescription} of the endpoint to be imported.
	 * @param properties optional properties (must be crushed by the description properties if conflict).
	 * @return An {@link ImportRegistration}.
	 */
	ImportRegistration importService(EndpointDescription description,Map<String, Object> properties);
	
	/**
	 * @return The configuration prefix used or defined by this {@link ImporterService}. (i.e <code>json-rpc,org.jabsorb,jax-rs</code>.
	 */
	List<String> getConfigPrefix();
	
	/**
     * @return All {@link ImportReference} of services imported through this
     *         service.
     */
    Collection<ImportReference> getAllImportReference();
	
}
