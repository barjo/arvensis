package org.ow2.chameleon.rose.pubsubhubbub.distributedhub;

import java.util.Set;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Distributed Hub.
 * 
 * @author Bartek
 * 
 */
public interface DistributedHub {

	String COMPONENT_NAME = "Rose_Pubsubhubbub.hub.distributed";
	String BOOTSTRAP_LINK_INSTANCE_PROPERTY = "bootstrap.link";
	String JERSEY_SERVLET_INSTANCE_PROPERTY = "alias";
	String JERSEY_SERVLET_ALIAS = "/Pubsubhubbub";
	String JERSEY_POST_LINK_HUBURL = "huburl";
	String JERSEY_POST_PARAMETER_ENDPOINT = "endpoint";

	/**
	 * Adds new endpoint system.
	 * 
	 * @param endpoint
	 *            {@link EndpointDescription} to add
	 * @param machineID
	 *            source machineID
	 */
	void addEndpoint(EndpointDescription endpoint, String machineID);

	/**
	 * Removes endpoint from system.
	 * 
	 * @param endpointID
	 *            {@link EndpointDescription} serviceID
	 * @param machineID
	 *            source machineID
	 */
	void removeEndpoint(long endpointID, String machineID);

	/**
	 * Add new connection to Distributed Hub.
	 * 
	 * @param link
	 *            new Distributed HUB url
	 */
	void addConnectedHub(String link);

	/**
	 * Removes connection from Distributed HUB.
	 * 
	 * @param link
	 *            Distributed HUB url to remove
	 */
	void removeConnectedHub(String link);

	/**
	 * @return all connected hubs
	 */
	Set<String> getConnectedHubs();

	/**
	 * @return Gateway Hub URI
	 */
	String getHubUri();

}