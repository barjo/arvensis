package org.ow2.chameleon.rose.pubsubhubbub.hub.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.pubsubhubbub.hub.Registrations;

/**
 * Keep all informations about registered rss topics with related endpoints and
 * subscribers with related endpoints filters.
 * 
 * @author Bartek
 * 
 */
public class RegistrationsImpl implements Registrations{

	// endpoint descriptions with publisher machine id
	private Map<EndpointDescription, String> endpoints;

	// connected publishers (topic rss url with machine id))
	private Map<String, String> publishers;

	// connected Hubs
	private Set<String> hubs;
	// topic url, endpoint descriptions
	// TODO remove
	private Map<String, Set<EndpointDescription>> topics;

	// connected subscribers with subscribed endpoints
	private Map<String, EndpointsByFilter> subscribers;
	private ReentrantReadWriteLock lock;

	/**
	 * Main constructor.
	 */
	public RegistrationsImpl() {
		endpoints = new HashMap<EndpointDescription, String>();
		publishers = new HashMap<String, String>();
		hubs = new HashSet<String>();
		topics = new ConcurrentHashMap<String, Set<EndpointDescription>>();
		subscribers = new ConcurrentHashMap<String, EndpointsByFilter>();
		lock = new ReentrantReadWriteLock();
	}

	/**
	 * Add new topic (publisher registers topic).
	 * 
	 * @param rssURL
	 *            publisher rss topic url
	 */
	public final void addTopic(final String rssURL, final String MachineID) {
		lock.writeLock().lock();
		try {
			publishers.put(rssURL, MachineID);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Add endpoint to topic.
	 * 
	 * @param rssUrl
	 *            publisher rss topic url
	 * @param endp
	 * @EndpointDescription description to add
	 */
	public final void addEndpointByTopicRssUrl(final String rssUrl,
			final EndpointDescription endp) {
		lock.writeLock().lock();
		endpoints.put(endp, publishers.get(rssUrl));
		// TODO remove
		// topics.get(rssUrl).add(endp);
		lock.writeLock().unlock();
	}

	/**
	 * Add endpoint to topic.
	 * 
	 * @param machineID
	 *            publisher machineID
	 * @param endp
	 * @EndpointDescription description to add
	 */
	public final void addEndpointByMachineID(final String machineID,
			final EndpointDescription endp) {
		lock.writeLock().lock();
		endpoints.put(endp, machineID);
		lock.writeLock().unlock();
	}

	/**
	 * Remove endpoint to topic.
	 * 
	 * @param rssUrl
	 *            publisher rss topic url
	 * @param endp
	 * @EndpointDescription description to remove
	 */
	public final void removeEndpoint(final String rssUrl,
			final EndpointDescription endp) {
		lock.writeLock().lock();
		endpoints.remove(endp);
		// TODO remove
		// topics.get(rssUrl).remove(endp);
		lock.writeLock().unlock();
	}

	/**
	 * Create new subscription with endpoint filter.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 * @param endpointFilter
	 *            filter to specify endpoints
	 */
	public final void addSubscription(final String callBackUrl,
			final String endpointFilter) {
		lock.writeLock().lock();
		try {
			subscribers.put(callBackUrl, new EndpointsByFilter(endpointFilter));
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Remove subscription.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 */
	public final void removeSubscribtion(final String callBackUrl) {
		lock.writeLock().lock();
		try {
			subscribers.remove(callBackUrl);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Get all endpoints which match a filter. Does not unlock read lock. Call
	 * {@link RegistrationsImpl#releaseReadLock()} after "consume" returned values}.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 * @return set contains @EndpointDescription
	 */
	public final Set<EndpointDescription> getEndpointsForCallBackUrl(
			final String callBackUrl) {
		lock.readLock().lock();
		Set<EndpointDescription> matchedEndpointDescriptions = new HashSet<EndpointDescription>();
		String filter = subscribers.get(callBackUrl).getFilter();
		for (EndpointDescription endpoint : getAllEndpoints().keySet()) {
			if (endpoint.matches(filter)) {
				matchedEndpointDescriptions.add(endpoint);
				subscribers.get(callBackUrl).addEndpoint(endpoint);
			}
		}
		return matchedEndpointDescriptions;
	}

	/**
	 * Provides information about registered subscribers whose endpoints filters
	 * matches given @EndpointDescription. Does not unlock read lock. Call
	 * {@link RegistrationsImpl#releaseReadLock()} after "consume" returned values}.
	 * 
	 * @param endp
	 * @EndpointDescription to search
	 * @return subscribers with matching endpoint filter
	 */
	public final Set<String> getSubscribersByEndpoint(
			final EndpointDescription endp) {
		Set<String> matchedSubscribers = new HashSet<String>();
		lock.readLock().lock();
		for (String subscriber : subscribers.keySet()) {

			if (endp.matches(subscribers.get(subscriber).getFilter())) {
				matchedSubscribers.add(subscriber);
				subscribers.get(subscriber).addEndpoint(endp);
			}
		}
		return matchedSubscribers;
	}

	/**
	 * Provides information about registered subscribers and their subscribed
	 * endpoints by checking if given publisher provides interested
	 * 
	 * @EndpointDescription.Does not unlock read lock. Call
	 *                           {@link RegistrationsImpl#releaseReadLock()} after
	 *                           "consume" returned values.
	 * 
	 * @param rssUrl
	 *            subscriber full RSS url address to send notifications
	 * @return @Map containing subscriber full url address to send notifications
	 *         and their @EndpointDescription
	 */
	public final Map<String, Set<EndpointDescription>> getSubscriberAndEndpointsByPublisherRssUrl(
			final String rssUrl) {

		Map<String, Set<EndpointDescription>> subscriberEndpoints = new ConcurrentHashMap<String, Set<EndpointDescription>>();
		lock.readLock().lock();
		for (String subscriber : subscribers.keySet()) {
			subscriberEndpoints.put(subscriber,
					new HashSet<EndpointDescription>());
			for (EndpointDescription endpoint : getEndpointsByMachineId(getTopicRssUrlByMachineID(rssUrl))) {
				if (subscribers.get(subscriber).getEndpoints()
						.contains(endpoint)) {
					subscriberEndpoints.get(subscriber).add(endpoint);
				}
			}
		}
		return subscriberEndpoints;
	}

	/**
	 * Provides information about registered subscribers and their subscribed
	 * endpoints by checking if given publisher provides interested
	 * 
	 * @EndpointDescription.Does not unlock read lock. Call
	 *                           {@link RegistrationsImpl#releaseReadLock()} after
	 *                           "consume" returned values.
	 * 
	 * @param machineID
	 *            subscriber machine ID
	 * @return @Map containing subscriber full url address to send notifications
	 *         and their @EndpointDescription
	 */
	public final Map<String, Set<EndpointDescription>> getSubscriberAndEndpointsByPublisherMachineID(
			final String machineID) {

		Map<String, Set<EndpointDescription>> subscriberEndpoints = new ConcurrentHashMap<String, Set<EndpointDescription>>();
		lock.readLock().lock();
		for (String subscriber : subscribers.keySet()) {
			subscriberEndpoints.put(subscriber,
					new HashSet<EndpointDescription>());
			for (EndpointDescription endpoint : getEndpointsByMachineId(machineID)) {
				if (subscribers.get(subscriber).getEndpoints()
						.contains(endpoint)) {
					subscriberEndpoints.get(subscriber).add(endpoint);
				}
			}
		}
		return subscriberEndpoints;
	}

	/**
	 * Provides all registered @EndpointSescription with MachineID.
	 * 
	 * @return all @EndpointSescription as Map collection, key as index
	 */
	public final Map<EndpointDescription, String> getAllEndpoints() {
		return endpoints;
	}

	/**
	 * Removes particular topic.
	 * 
	 * @param rssURL
	 *            publisher rss topic url to delete
	 */
	public final void clearTopic(final String rssURL) {
		lock.readLock().lock();
		endpoints.get(publishers.get(rssURL));
		for (Entry<EndpointDescription, String> entry : endpoints.entrySet()) {
			if (entry.getValue().equals(rssURL)) {
				for (String subscriber : subscribers.keySet()) {
					subscribers.get(subscriber).getEndpoints()
							.remove(entry.getKey());
				}
			}
		}
		publishers.remove(rssURL);
		lock.readLock().unlock();
	}

	/**
	 * Removes particular @EndpointDescription from subscriber registration.
	 * 
	 * @param callBackUrl
	 *            subscriber full url address to send notifications
	 * @param edp
	 * @EndpointSescription to remove
	 */
	public final void removeInterestedEndpoint(final String callBackUrl,
			final EndpointDescription edp) {
		subscribers.get(callBackUrl).removeEndpoint(edp);
	}

	/**
	 * Releases read lock for current thread
	 */
	public final void releaseReadLock() {
		lock.readLock().unlock();
	}

	private final Set<EndpointDescription> getEndpointsByMachineId(
			String machineID) {
		Set<EndpointDescription> endp = new HashSet<EndpointDescription>();
		for (Entry<EndpointDescription, String> endpointEntry : endpoints
				.entrySet()) {
			if (endpointEntry.getValue().equals(machineID)) {
				endp.add(endpointEntry.getKey());
			}
		}
		return endp;

	}

	private final String getTopicRssUrlByMachineID(String machineID) {

		for (Entry<String, EndpointsByFilter> subscribersEntry : subscribers
				.entrySet()) {
			if (subscribersEntry.getValue().equals(machineID)) {
				return subscribersEntry.getKey();
			}
		}
		return null;
	}

	/**
	 * Keeps connection between particular filter and @EndpointSescription which
	 * satisfies it.
	 * 
	 * @author Bartek
	 * 
	 */
	private static class EndpointsByFilter {
		private String filter;
		private Set<EndpointDescription> matchedEndpoints;

		public EndpointsByFilter(final String pFilter) {
			this.filter = pFilter;
			matchedEndpoints = new HashSet<EndpointDescription>();
		}

		public String getFilter() {
			return filter;
		}

		public void addEndpoint(final EndpointDescription endp) {
			matchedEndpoints.add(endp);
		}

		public void removeEndpoint(final EndpointDescription endp) {
			matchedEndpoints.remove(endp);
		}

		public Set<EndpointDescription> getEndpoints() {
			return matchedEndpoints;
		}

	}
}
