package org.ow2.chameleon.rose.pubsubhubbub.subscriber;

import java.io.IOException;

import javax.servlet.ServletException;

import org.osgi.service.http.NamespaceException;

/**
 * Subscriber interface with all constants.
 * 
 * @author Bartek
 * 
 */
public interface Subscriber {

	String COMPONENT_NAME = "Rose_Pubsubhubbub.subscriber";
	String INSTANCE_PROPERTY_CALLBACK_URL = "callback.url";
	String INSTANCE_PROPERTY_HUB_URL = "hub.url";
	String INSTANCE_PROPERTY_ENDPOINT_FILTER = "endpoint.filter";

	void start();

	void stop();

}