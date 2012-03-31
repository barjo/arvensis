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

	// connected publishers (topic rss url with machine id/callBackurl))
	private Map<String, PublisherInfo> publishers;

	// connected subscribers with subscribed endpoints
	private Map<String, SubscriberInfo> subscribers;
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
		publishers = new HashMap<String, RegistrationsImpl.PublisherInfo>();
		subscribers = new ConcurrentHashMap<String, SubscriberInfo>();
		lock = new ReentrantReadWriteLock();
		sendSubscription = new SendSubscription(logger, json);
	}

	public final void addTopic(final String rssURL, final String machineID,
			final String callbackUrl) {
		lock.writeLock().lock();
		try {
			publishers.put(rssURL, new PublisherInfo(callbackUrl, machineID));
			// publishers2.put(rssURL, machineID);
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
			if (endpoint.getValue().equals(
					publishers.get(rssUrl).getMachineID())) {
				// store endpoint to remove
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
		endpoints.put(endp, publishers.get(rssUrl).getMachineID());
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
			final String endpointFilter, final String machineID) {
		SubscriberInfo endpointsByFiler = new SubscriberInfo(endpointFilter,
				machineID);
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

	public final String getPublisherMachineIdByRssUrl(String rssUrl) {
		return publishers.get(rssUrl).getMachineID();

	}

	public final Map<String, String> getSubscribers() {
		Map<String, String> subscribersMap = new HashMap<String, String>();
		for (Entry<String, SubscriberInfo> subscriber : subscribers.entrySet()) {
			subscribersMap.put(subscriber.getKey(), subscriber.getValue()
					.getMachineID());
		}
		return subscribersMap;
	}

	public final Map<String, String> getPublishers() {
		Map<String, String> publishersMap = new HashMap<String, String>();
		for (Entry<String, PublisherInfo> publisher : publishers.entrySet()) {
			publishersMap.put(publisher.getValue().getCallBackUrl(), publisher
					.getValue().getMachineID());
		}
		return publishersMap;
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
	 * satisfies it. Stores machineID
	 * 
	 * @author Bartek
	 * 
	 */
	private static class SubscriberInfo {
		private String filter;
		private String machineID;
		private Set<EndpointDescription> matchedEndpoints;

		public SubscriberInfo(final String pFilter, final String pMachineID) {
			this.filter = pFilter;
			this.machineID = pMachineID;
			matchedEndpoints = new HashSet<EndpointDescription>();
		}

		public String getFilter() {
			return filter;
		}

		public String getMachineID() {
			return machineID;
		}

		public void addEndpoint(final EndpointDescription endp) {
			matchedEndpoints.add(endp);
		}

		public boolean removeEndpoint(final EndpointDescription endp) {
			return matchedEndpoints.remove(endp);
		}

	}

	/**
	 * Keeps Publisher informations: machineID, callBack, rssUrl
	 * 
	 * @author Bartek
	 * 
	 */
	private static class PublisherInfo {
		private String machineID;
		private String callBackUrl;

		public PublisherInfo(String pCallBackUrl, String pMachineID) {
			this.machineID = pMachineID;
			this.callBackUrl = pCallBackUrl;
		}

		public String getMachineID() {
			return machineID;
		}

		public String getCallBackUrl() {
			return callBackUrl;
		}

	}
}
