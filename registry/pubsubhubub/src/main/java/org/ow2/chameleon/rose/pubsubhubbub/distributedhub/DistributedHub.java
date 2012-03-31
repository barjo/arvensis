package org.ow2.chameleon.rose.pubsubhubbub.distributedhub;

import java.text.ParseException;

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
	String JERSEY_ALIAS_INSTANCE_PROPERTY = "alias";
	String JERSEY_DEFAULT_SERVLET_ALIAS = "/Pubsubhubbub";
	String JERSEY_POST_LINK_HUBURL = "huburl";
	String JERSEY_POST_PARAMETER_SUBSCRIBER = "subscriber";
	String JERSEY_POST_PARAMETER_PUBLISHER = "publisher";
	String JERSEY_POST_PARAMETER_CALLBACKURL = "callbackurl";
	String JERSEY_POST_PARAMETER_ENDPOINT = "endpoint";

	/**
	 * Adds new endpoint to distributed system. Send to all connected hubs
	 * 
	 * @param endpoint
	 *            {@link EndpointDescription} to add
	 * @param publisherMachineID
	 *            source machineID
	 */
	void addEndpoint(EndpointDescription endpoint, String publisherMachineID);

	/**
	 * Adds new endpoint to distributed system. Send to all connected hubs
	 * excluding one from parameter
	 * 
	 * @param endpoint
	 *            {@link EndpointDescription} to add
	 * @param publisherMachineID
	 *            source machineID
	 * @param excludeMachineID
	 *            machineID to exclude
	 */
	void addEndpoint(EndpointDescription endpoint, String publisherMachineID,
			String excludeMachineID);

	/**
	 * Removes endpoint from system. Send to all connected hubs
	 * 
	 * @param endpointID
	 *            {@link EndpointDescription} serviceID
	 * @param publisherMachineID
	 *            source machineID
	 */
	void removeEndpoint(long endpointID, String publisherMachineID);

	/**
	 * Removes endpoint from system.Send to all connected hubs excluding one
	 * from parameter
	 * 
	 * @param endpointID
	 *            {@link EndpointDescription} serviceID
	 * @param publisherMachineID
	 *            source machineID
	 * @param excludeMachineID
	 *            machineID to exclude
	 */
	void removeEndpoint(long endpointID, String publisherMachineID,
			String excludeMachineID);

	/**
	 * @return Gateway Hub URI
	 */
	String getHubUri();


	/**
	 * @param uri Distributed Hub URI
	 * @throws ParseException
	 */
	void establishConnection(String uri) throws ParseException;

	/**
	 * @return machineID from Rose
	 */
	String getMachineID();

	/**
	 * Send informations about connection between Pubsubhubbub and new
	 * subscriber.
	 * 
	 * @param subscriberMachineID
	 *            subscriber machineID
	 * @param callBackUrl
	 *            subscriber callBackUrl
	 */
	void addBackupSubscriber(String subscriberMachineID, String callBackUrl);

	/**
	 * Send informations about closed connection between Pubsubhubbub and
	 * subscriber.
	 * 
	 * @param subscriberMachineID
	 *            subscriber machineID
	 */
	void removeBackupSubscriber(String subscriberMachineID);

	/**
	 * Send informations about connection between Pubsubhubbub and new
	 * subscriber.
	 * 
	 * @param publisherMachineID
	 *            publisher machineID
	 * @param callBackUrl
	 *            publisher callBackUrl
	 */
	void addBackupPublisher(String publisherMachineID, String callBackUrl);

	/**
	 * Send informations about closed connection between Pubsubhubbub and new
	 * publisher.
	 * 
	 * @param publisherMachineID
	 *            publisher machineID
	 */
	void removeBackupPublisher(String publisherMachineID);

	/**
	 * @return all backup subscribers(machineID, callBackUrl) on every connected
	 *         machine
	 */
	HubBackups getHubBackups();

}