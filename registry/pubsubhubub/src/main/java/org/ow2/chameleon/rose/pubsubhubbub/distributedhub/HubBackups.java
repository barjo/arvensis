package org.ow2.chameleon.rose.pubsubhubbub.distributedhub;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Stored all backup data
 * 
 * @author Bartek
 * 
 */
public interface HubBackups {

	void addConnectedHub(String machineID, String link);

	void removeConnectedHubByID(String machineID);

	void removeConnectedHubByURI(String URI);
	
	Set<String> getAllHubsLink();

	Set<String> getAllHubsLink(String excludeMachineID);

	void addSubscriber(String hubMachineID, String susbcriberMachineID,
			String callbackUrl);

	void removeSubscriber(String hubMachineID, String susbcriberMachineID);

	/**
	 * @return pubsubhubbub machineID with subscriber callBackurls
	 */
	Map<String, Collection<String>> getSubscribers();

	Map<String, String> getSubscribers(String link);

	void addPublisher(String hubMachineID, String publisherMachineID,
			String callbackUrl);

	void removePublisher(String hubMachineID, String publisherMachineID);

	/**
	 * @return pubsubhubbub machineID with publisher callBackurls
	 */
	Map<String, Collection<String>> getPublishers();

	/**
	 * @return pubsubhubbub machineID with publisher callBackurls
	 */
	Map<String, String> getPublishers(String link);

}