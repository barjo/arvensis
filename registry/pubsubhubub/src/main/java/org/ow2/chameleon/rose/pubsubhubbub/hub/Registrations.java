package org.ow2.chameleon.rose.pubsubhubbub.hub;

import java.util.Map;
import java.util.Set;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.pubsubhubbub.hub.internal.RegistrationsImpl;

public interface Registrations {

	/**
	 * Add new topic (publisher registers topic).
	 * 
	 * @param rssURL
	 *            publisher rss topic url
	 */
	public void addTopic(final String rssURL, final String MachineID);

	
	/**
	 * Add endpoint to topic.
	 * 
	 * @param rssUrl
	 *            publisher rss topic url
	 * @param endp
	 * @EndpointDescription description to add
	 */
	public void addEndpointByTopicRssUrl(final String rssUrl,
			final EndpointDescription endp);

	/**
	 * Add endpoint to topic.
	 * 
	 * @param machineID
	 *            publisher machineID
	 * @param endp
	 * @EndpointDescription description to add
	 */
	public void addEndpointByMachineID(final String machineID,
			final EndpointDescription endp);

	/**
	 * Remove endpoint to topic.
	 * 
	 * @param rssUrl
	 *            publisher rss topic url
	 * @param endp
	 * @EndpointDescription description to remove
	 */
	public void removeEndpointByTopicRssUrl(final String rssUrl,
			final EndpointDescription endp);

	/**
	 * Create new subscription with endpoint filter.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 * @param endpointFilter
	 *            filter to specify endpoints
	 */
	public void addSubscriber(final String callBackUrl,
			final String endpointFilter);

	/**
	 * Remove subscription.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 */
	public void removeSubscriber(final String callBackUrl);

	/**
	 * Provides all registered @EndpointSescription with MachineID.
	 * 
	 * @return all @EndpointSescription as Map collection, key as index
	 */
	public Map<EndpointDescription, String> getAllEndpoints();

}
