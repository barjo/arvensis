package org.ow2.chameleon.rose.pubsubhubbub.publisher;


/**
 * Publisher interface with all constants.
 * 
 * @author Bartek
 * 
 */
public interface Publisher {

	String COMPONENT_NAME = "Rose_Pubsubhubbub.publisher";
	String INSTANCE_PROPERTY_RSS_URL = "rss.url";
	String INSTANCE_PROPERTY_HUB_URL = "hub.url";

	/**
	 * Validate method.
	 */
	void start();

	/**
	 * Invalidate method.
	 */
	void stop();

}
