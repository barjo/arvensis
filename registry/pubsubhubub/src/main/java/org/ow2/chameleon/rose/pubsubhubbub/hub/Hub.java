package org.ow2.chameleon.rose.pubsubhubbub.hub;

import java.util.Map;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;

/**
 * HUB interface with all constants.
 * 
 * @author Bartek
 * 
 */
public interface Hub {

	String COMPONENT_NAME = "Rose_Pubsubhubbub.hub";
	String INSTANCE_PROPERTY_HUB_URL = "hub.url";

	/**
	 * Validate method.
	 */
	void start();

	/**
	 * Invalidate method.
	 */
	void stop();
	
	/**
	 * @return {@link Registrations}
	 */
	Registrations getRegistrations();
	
	/**Check given properties and provide {@link EndpointDescription}
	 * @param map {@link EndpointDescription} properties parsed by {@link JSONService}
	 * @return proper {@link EndpointDescription}
	 */
	EndpointDescription getEndpointDescriptionFromJSON(
			final Map<String, Object> map);

}
