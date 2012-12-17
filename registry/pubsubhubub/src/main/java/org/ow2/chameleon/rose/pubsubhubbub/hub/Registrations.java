package org.ow2.chameleon.rose.pubsubhubbub.hub;

import java.util.Map;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

/**
 * Keep all informations about registered rss topics with related endpoints and
 * subscribers with related endpoints filters.
 * 
 * @author Bartek
 * 
 */
public interface Registrations {

	/**
	 * Add new topic (publisher registers topic).
	 * 
	 * @param rssURL
	 *            publisher rss topic url
	 * @param machineID
	 *            ID of given machine
	 * @param callbackUrl
	 *            publisher full url address to send notifications
	 */
	void addTopic(final String rssURL, final String machineID,
			final String callbackUrl);

	/**
	 * Add endpoint to topic.
	 * 
	 * @param rssUrl
	 *            publisher rss topic url
	 * @param endp
	 *            {@link EndpointDescription} description to add
	 */
	void addEndpointByTopicRssUrl(final String rssUrl,
			final EndpointDescription endp);

	/**
	 * Add endpoint to topic.
	 * 
	 * @param machineID
	 *            publisher machineID
	 * @param endp
	 *            the {@link EndpointDescription} description to add
	 * @return true if new {@link EndpointDescription} added; false otherwise
	 */
	boolean addEndpointByMachineID(final String machineID,
			final EndpointDescription endp);

	/**
	 * Remove endpoint to topic.
	 * 
	 * @param rssUrl
	 *            publisher rss topic url
	 * @param endp
	 *            the {@link EndpointDescription} description to remove
	 */
	void removeEndpointByTopicRssUrl(final String rssUrl,
			final EndpointDescription endp);

	/**
	 * Create new subscription with endpoint filter.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 * @param endpointFilter
	 *            filter to specify endpoints
	 * @param machineID
	 *            subscriber machineID
	 */
	void addSubscriber(final String callBackUrl, final String endpointFilter,
			final String machineID);

	/**
	 * Remove subscription.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 */
	void removeSubscriber(final String callBackUrl);

	/**
	 * Provides all registered @EndpointSescription with MachineID.
	 * 
	 * @return all @EndpointSescription as Map collection, key as index
	 */
	Map<EndpointDescription, String> getAllEndpoints();

	/**
	 * Removes endpoint.
	 * 
	 * @param machineID
	 *            publisher machineId
	 * @param endpointId
	 *            Endpoint id
	 * @return if endpoints deleted
	 */
	boolean removeEndpoint(String machineID, long endpointId);

	/**
	 * Retrieves publishers machineID by rss url
	 * 
	 * @param publisher
	 *            publisher`s RSS url
	 * @return publisher`s machineID
	 */
	String getPublisherMachineIdByRssUrl(String publisher);

	/**
	 * @return registered subscribers, connection url and machineID
	 */
	Map<String, String> getSubscribers();

	/**
	 * @return registered publishers, connection url and machineID
	 */
	Map<String, String> getPublishers();

}
