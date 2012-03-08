package org.ow2.chameleon.rose.pubsubhubbub.hub;

import org.ow2.chameleon.rose.pubsubhubbub.hub.internal.RegistrationsImpl;

/**
 * HUB interface with all constants.
 * 
 * @author Bartek
 * 
 */
public interface Hub {

	String COMPONENT_NAME = "Rose_Pubsubhubbub.hub";
	String INSTANCE_PROPERTY_HUB_URL = "hub.url";

	void start();

	void stop();
	
	public RegistrationsImpl getRegistrations();

}
