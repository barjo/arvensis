package org.ow2.chameleon.rose.pubsubhubbub.hub.internal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class Registrations {

	// topic name, endpoint descriptions
	private Map<String, Set<EndpointDescription>> topics;
	// subscriber name(callBackUrl), filter
	private Map<String, EndpointsByFilter> subscribers;
	private ReentrantReadWriteLock lock;

	public Registrations() {
		topics = new ConcurrentHashMap<String, Set<EndpointDescription>>();
		subscribers = new ConcurrentHashMap<String, EndpointsByFilter>();
		lock = new ReentrantReadWriteLock();
	}

	public final void addTopic(final String rssURL) {
		lock.writeLock().lock();
		try {
			topics.put(rssURL, new HashSet<EndpointDescription>());
		} finally {
			lock.writeLock().unlock();
		}
	}

	public final void addEndpoint(final String rssUrl, final EndpointDescription endp) {
		lock.writeLock().lock();
		topics.get(rssUrl).add(endp);
		lock.writeLock().unlock();
	}

	public final void removeEndpoint(final String rssUrl, final EndpointDescription endp) {
		lock.writeLock().lock();
		topics.get(rssUrl).remove(endp);
		lock.writeLock().unlock();
	}

	public final void addSubscrition(final String callBackUrl, final String endpointFilter) {
		lock.writeLock().lock();
		try {
			subscribers.put(callBackUrl, new EndpointsByFilter(endpointFilter));
		} finally {
			lock.writeLock().unlock();
		}
	}

	public final void removeSubscribtion(final String callBackUrl) {
		lock.writeLock().lock();
		try {
			subscribers.remove(callBackUrl);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Get all endpoints which match a filter.
	 * 
	 * @param callBackUrl subscriber full url address to send notifications 
	 * @return set contains @EndpointDescription 
	 */
	public final Set<EndpointDescription> getEndpointsForCallBackUrl(
			final String callBackUrl) {
		lock.readLock().lock();
		Set<EndpointDescription> matchedEndpointDescriptions = new HashSet<EndpointDescription>();
		String filter = subscribers.get(callBackUrl).getFilter();
		for (EndpointDescription endpoint : getAllEndpoints()) {
			if (endpoint.matches(filter)) {
				matchedEndpointDescriptions.add(endpoint);
				subscribers.get(callBackUrl).addEndpoint(endpoint);
			}
		}
		lock.readLock().unlock();
		return matchedEndpointDescriptions;
	}

	public final Set<String> getSubscribersByEndpoint(final EndpointDescription endp) {
		Set<String> matchedSubscribers = new HashSet<String>();
		lock.readLock().lock();
		for (String subscriber : subscribers.keySet()) {

			if (endp.matches(subscribers.get(subscriber).getFilter())) {
				matchedSubscribers.add(subscriber);
				subscribers.get(subscriber).addEndpoint(endp);
			}
		}
		lock.readLock().unlock();
		return matchedSubscribers;
	}

	public final Map<String, Set<EndpointDescription>> getEndpointsAndSubscriberByPublisher(
			final String callBackUrl) {

		Map<String, Set<EndpointDescription>> subscriberEndpoints = new ConcurrentHashMap<String, Set<EndpointDescription>>();
		lock.readLock().lock();
		for (String subscriber : subscribers.keySet()) {
			subscriberEndpoints.put(subscriber,
					new HashSet<EndpointDescription>());
			for (EndpointDescription endpoint : topics.get(callBackUrl)) {
				if (subscribers.get(subscriber).getEndpoints()
						.contains(endpoint)) {
					subscriberEndpoints.get(subscriber).add(endpoint);
				}
			}
		}
		lock.readLock().unlock();
		return subscriberEndpoints;
	}

	public final Set<EndpointDescription> getAllEndpoints() {

		Set<EndpointDescription> allEndpoints = new HashSet<EndpointDescription>();
		lock.readLock().lock();
		for (String publisher : topics.keySet()) {
			allEndpoints.addAll(topics.get(publisher));
		}
		lock.readLock().unlock();
		return allEndpoints;
	}

	public final void clearTopic(final String rssURL) {
		lock.readLock().lock();
		for (EndpointDescription endpoint : topics.get(rssURL)) {

			for (String subscriber : subscribers.keySet()) {
				subscribers.get(subscriber).getEndpoints().remove(endpoint);
			}
		}
		topics.remove(rssURL);
		lock.readLock().unlock();
	}

	public final void removeInterestedEndpoint(final String callBackUrl,
			final EndpointDescription edp) {
		lock.writeLock().lock();
		subscribers.get(callBackUrl).removeEndpoint(edp);
		lock.writeLock().unlock();
	}

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
