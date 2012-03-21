package org.ow2.chameleon.rose.pubsubhubbub.hub.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.pubsubhubbub.hub.Registrations;

import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED;

public class RegistrationsImpl implements Registrations {

	// publisher endpoint description with endpoint descriptions machine id
	private Map<EndpointDescription, String> endpoints;

	// connected publishers (topic rss url with machine id))
	private Map<String, String> publishers;

	// connected subscribers with subscribed endpoints
	private Map<String, EndpointsByFilter> subscribers;
	private ReentrantReadWriteLock lock;

	private SendSubscription sendSubscription;

	/**
	 * Main constructor.
	 * 
	 * @param json
	 *            {@link JSONService}
	 * @param logger
	 *            {@link LogService}
	 */
	public RegistrationsImpl(final JSONService json, final LogService logger) {
		endpoints = new HashMap<EndpointDescription, String>();
		publishers = new HashMap<String, String>();
		subscribers = new ConcurrentHashMap<String, EndpointsByFilter>();
		lock = new ReentrantReadWriteLock();
		sendSubscription = new SendSubscription(logger, json);
	}

	public final void addTopic(final String rssURL, final String machineID) {
		lock.writeLock().lock();
		try {
			publishers.put(rssURL, machineID);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public final void removeTopic(String rssUrl) {
		Set<String> matchedSubscribers;
		Set<EndpointDescription> endpointsToRemove = new HashSet<EndpointDescription>();
		lock.writeLock().lock();
		for (Entry<EndpointDescription, String> endpoint : endpoints.entrySet()) {
			// find all endpoints registered by publisher
			if (endpoint.getValue().equals(publishers.get(rssUrl))) {
				//store endpoint to remove
				endpointsToRemove.add(endpoint.getKey());
				// get subscribers who use this endpoint
				matchedSubscribers = this.getSubscribersByEndpoint(
						endpoint.getKey(), false);
				if (!(matchedSubscribers.isEmpty())) {
					sendSubscription.sendSubscriptions(matchedSubscribers,
							endpoint.getKey(),
							HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED);
				}

			}
		}
		// remove registration of endpoint
		endpoints.keySet().removeAll(endpointsToRemove);
		lock.writeLock().unlock();
	}

	public final void addEndpointByTopicRssUrl(final String rssUrl,
			final EndpointDescription endp) {
		Set<String> matchedSubscribers;
		lock.writeLock().lock();
		endpoints.put(endp, publishers.get(rssUrl));
		matchedSubscribers = getSubscribersByEndpoint(endp, true);
		if (!(matchedSubscribers.isEmpty())) {
			sendSubscription.sendSubscriptions(matchedSubscribers, endp,
					HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED);
		}
		lock.writeLock().unlock();
	}

	public final boolean addEndpointByMachineID(final String machineID,
			final EndpointDescription endp) {
		Set<String> matchedSubscribers;
		lock.writeLock().lock();
		try {
			// check if endpoint already exists
			if (endpoints.containsKey(endp)) {
				return false;
			}
			endpoints.put(endp, machineID);
			matchedSubscribers = getSubscribersByEndpoint(endp, true);
			if (!(matchedSubscribers.isEmpty())) {
				sendSubscription.sendSubscriptions(matchedSubscribers, endp,
						HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED);
			}
		} finally {
			lock.writeLock().unlock();
		}
		return true;
	}

	public final void removeEndpointByTopicRssUrl(final String rssUrl,
			final EndpointDescription endp) {
		Set<String> matchedSubscribers;
		lock.writeLock().lock();
		try {
			endpoints.remove(endp);
			matchedSubscribers = getSubscribersByEndpoint(endp, false);
			if (!(matchedSubscribers.isEmpty())) {
				sendSubscription.sendSubscriptions(matchedSubscribers, endp,
						HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	public final void addSubscriber(final String callBackUrl,
			final String endpointFilter) {
		EndpointsByFilter endpointsByFiler = new EndpointsByFilter(
				endpointFilter);
		lock.writeLock().lock();
		try {
			for (EndpointDescription endpoint : endpoints.keySet()) {
				if (endpoint.matches(endpointFilter)) {
					endpointsByFiler.addEndpoint(endpoint);

					sendSubscription.sendSubscriptions(new HashSet<String>(
							Arrays.asList(callBackUrl)), endpoint,
							HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED);
				}
			}
			subscribers.put(callBackUrl, endpointsByFiler);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public final void removeSubscriber(final String callBackUrl) {
		lock.writeLock().lock();
		try {
			subscribers.remove(callBackUrl);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public final Map<EndpointDescription, String> getAllEndpoints() {
		return endpoints;
	}

	public final boolean removeEndpoint(String machineID, long endpointId) {
		// find endpoint by his endpointID and publisher
		lock.writeLock().lock();
		for (EndpointDescription endp : getEndpointsByMachineId(machineID)) {
			if (endp.getServiceId() == endpointId) {
				this.removeEndpointByMachineID(machineID, endp);
				lock.writeLock().unlock();
				return true;
			}

		}
		lock.writeLock().unlock();
		return false;

	}

	public final String getPublisherMachineIdByRssUrl(String machineID) {
		return publishers.get(machineID);

	}

	private void removeEndpointByMachineID(String machineID,
			EndpointDescription endp) {

		Set<String> matchedSubscribers;
		lock.writeLock().lock();
		try {
			endpoints.remove(endp);
			matchedSubscribers = getSubscribersByEndpoint(endp, false);
			if (!(matchedSubscribers.isEmpty())) {
				sendSubscription.sendSubscriptions(matchedSubscribers, endp,
						HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	private Set<EndpointDescription> getEndpointsByMachineId(String machineID) {
		Set<EndpointDescription> endp = new HashSet<EndpointDescription>();
		for (Entry<EndpointDescription, String> endpointEntry : endpoints
				.entrySet()) {
			if (endpointEntry.getValue().equals(machineID)) {
				endp.add(endpointEntry.getKey());
			}
		}
		return endp;

	}

	/**
	 * @param endp
	 * @param option
	 *            if true add endpoint, otherwise if false, remove endpoint from
	 *            every matched publisher
	 * @return
	 */
	private Set<String> getSubscribersByEndpoint(
			final EndpointDescription endp, final boolean option) {
		Set<String> matchedSubscribers = new HashSet<String>();
		for (String subscriber : subscribers.keySet()) {

			if (endp.matches(subscribers.get(subscriber).getFilter())) {
				// check option
				if (option) {
					subscribers.get(subscriber).addEndpoint(endp);
					matchedSubscribers.add(subscriber);
					// check if subscriber still registers given endpoint
				} else if (subscribers.get(subscriber).removeEndpoint(endp)) {
					matchedSubscribers.add(subscriber);
				}
			}
		}
		return matchedSubscribers;
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

		public boolean removeEndpoint(final EndpointDescription endp) {
			return matchedEndpoints.remove(endp);
		}

	}

}
