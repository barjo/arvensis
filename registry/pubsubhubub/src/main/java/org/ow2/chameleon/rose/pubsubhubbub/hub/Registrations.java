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

	/**Add new topic (publisher registers topic).
	 * @param rssURL publisher rss topic url
	 * @param machineID ID of given machine
	 */
	void addTopic(final String rssURL, final String machineID);

	/**
	 * Add endpoint to topic.
	 * 
	 * @param rssUrl
	 *            publisher rss topic url
	 * @param endp
	 * {@link EndpointDescription} description to add
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
	 */
	void addEndpointByMachineID(final String machineID,
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
	 */
	void addSubscriber(final String callBackUrl,
			final String endpointFilter);

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

}
