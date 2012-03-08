package org.ow2.chameleon.rose.pubsubhubbub.hub;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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
	public void removeEndpoint(final String rssUrl,
			final EndpointDescription endp);

	/**
	 * Create new subscription with endpoint filter.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 * @param endpointFilter
	 *            filter to specify endpoints
	 */
	public void addSubscription(final String callBackUrl,
			final String endpointFilter);

	/**
	 * Remove subscription.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 */
	public void removeSubscribtion(final String callBackUrl);

	/**
	 * Get all endpoints which match a filter. Does not unlock read lock. Call
	 * {@link RegistrationsImpl#releaseReadLock()} after "consume" returned
	 * values}.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 * @return set contains @EndpointDescription
	 */
	public Set<EndpointDescription> getEndpointsForCallBackUrl(
			final String callBackUrl);

	/**
	 * Provides information about registered subscribers whose endpoints filters
	 * matches given @EndpointDescription. Does not unlock read lock. Call
	 * {@link RegistrationsImpl#releaseReadLock()} after "consume" returned
	 * values}.
	 * 
	 * @param endp
	 * @EndpointDescription to search
	 * @return subscribers with matching endpoint filter
	 */
	public Set<String> getSubscribersByEndpoint(final EndpointDescription endp);

	/**
	 * Provides information about registered subscribers and their subscribed
	 * endpoints by checking if given publisher provides interested
	 * 
	 * @EndpointDescription.Does not unlock read lock. Call
	 *                           {@link RegistrationsImpl#releaseReadLock()}
	 *                           after "consume" returned values.
	 * 
	 * @param rssUrl
	 *            subscriber full RSS url address to send notifications
	 * @return @Map containing subscriber full url address to send notifications
	 *         and their @EndpointDescription
	 */
	public Map<String, Set<EndpointDescription>> getSubscriberAndEndpointsByPublisherRssUrl(
			final String rssUrl);

	/**
	 * Provides information about registered subscribers and their subscribed
	 * endpoints by checking if given publisher provides interested
	 * 
	 * @EndpointDescription.Does not unlock read lock. Call
	 *                           {@link RegistrationsImpl#releaseReadLock()}
	 *                           after "consume" returned values.
	 * 
	 * @param machineID
	 *            subscriber machine ID
	 * @return @Map containing subscriber full url address to send notifications
	 *         and their @EndpointDescription
	 */
	public Map<String, Set<EndpointDescription>> getSubscriberAndEndpointsByPublisherMachineID(
			final String machineID);

	/**
	 * Provides all registered @EndpointSescription with MachineID.
	 * 
	 * @return all @EndpointSescription as Map collection, key as index
	 */
	public Map<EndpointDescription, String> getAllEndpoints();

	/**
	 * Removes particular topic.
	 * 
	 * @param rssURL
	 *            publisher rss topic url to delete
	 */
	public void clearTopic(final String rssURL);

	/**
	 * Removes particular @EndpointDescription from subscriber registration.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 * @param edp
	 * @EndpointSescription to remove
	 */
	public void removeInterestedEndpoint(final String callBackUrl,
			final EndpointDescription edp);

	/**
	 * Releases read lock for current thread
	 */
	public void releaseReadLock();

}
