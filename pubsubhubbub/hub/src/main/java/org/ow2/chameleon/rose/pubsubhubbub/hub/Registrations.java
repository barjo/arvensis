package org.ow2.chameleon.rose.pubsubhubbub.hub;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.remoteserviceadmin.EndpointDescription;

public class Registrations {

	// topic name, endpoint descriptions
	private Map<String, Set<EndpointDescription>> topics;
	// subscriber name(callBackUrl), filter
	private Map<String, EndpointsByFilter> subscribers;

	public Registrations() {
		topics = new ConcurrentHashMap<String, Set<EndpointDescription>>();
		subscribers = new ConcurrentHashMap<String, EndpointsByFilter>();
	}

	public void addTopic(String rssURL) {
		topics.put(rssURL, new HashSet<EndpointDescription>());
	}

	public void addEndpoint(String rssUrl, EndpointDescription endp) {
		topics.get(rssUrl).add(endp);
	}

	public void removeEndpoint(String rssUrl, EndpointDescription endp) {
		topics.get(rssUrl).remove(endp);
	}

	public void addSubscrition(String callBackUrl, String endpointFilter) {
		subscribers.put(callBackUrl, new EndpointsByFilter(endpointFilter));
	}

	public void removeSubscribtion(String callBackUrl) {
		subscribers.remove(callBackUrl);
	}

	/**
	 * Get all endpoints which match a filter
	 * 
	 * @param callBackUrl
	 * @return
	 */
	public Set<EndpointDescription> getEndpointsForCallBackUrl(
			String callBackUrl) {

		Set<EndpointDescription> matchedEndpointDescriptions = new HashSet<EndpointDescription>();
		String filter = subscribers.get(callBackUrl).getFilter();

		for (EndpointDescription endpoint : getAllEndpoints()) {
			if (endpoint.matches(filter)) {
				matchedEndpointDescriptions.add(endpoint);
				subscribers.get(callBackUrl).addEndpoint(endpoint);
			}
		}
		return matchedEndpointDescriptions;
	}

	public Set<String> getSubscribersByEndpoint(EndpointDescription endp) {
		Set<String> matchedSubscribers = new HashSet<String>();

		for (String subscriber : subscribers.keySet()) {

			if (endp.matches(subscribers.get(subscriber).getFilter())) {
				matchedSubscribers.add(subscriber);
				subscribers.get(subscriber).addEndpoint(endp);
			}
		}
		return matchedSubscribers;
	}

	public Map<String, Set<EndpointDescription>> getEndpointsAndSubscriberByPublisher(
			String callBackUrl) {

		Map<String, Set<EndpointDescription>> subscriberEndpoints = new ConcurrentHashMap<String, Set<EndpointDescription>>();
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
		return subscriberEndpoints;
	}

	public Set<EndpointDescription> getAllEndpoints() {

		Set<EndpointDescription> allEndpoints = new HashSet<EndpointDescription>();
		for (String publisher : topics.keySet()) {
			allEndpoints.addAll(topics.get(publisher));
		}

		return allEndpoints;
	}

	public void clearTopic(String rssURL) {
		for (EndpointDescription endpoint : topics.get(rssURL)) {

			for (String subscriber : subscribers.keySet()) {
				subscribers.get(subscriber).getEndpoints().remove(endpoint);
			}
		}
		topics.remove(rssURL);

	}

	public void removeInterestedEndpoint(String callBackUrl,
			EndpointDescription edp) {
		subscribers.get(callBackUrl).removeEndpoint(edp);
	}

	private class EndpointsByFilter {
		private String filter;
		private Set<EndpointDescription> matchedEndpoints;

		public EndpointsByFilter(String filter) {
			this.filter = filter;
			matchedEndpoints = new HashSet<EndpointDescription>();
		}

		public String getFilter() {
			return filter;
		}

		public void addEndpoint(EndpointDescription endp) {
			matchedEndpoints.add(endp);
		}

		public void removeEndpoint(EndpointDescription endp) {
			matchedEndpoints.remove(endp);
		}

		public Set<EndpointDescription> getEndpoints() {
			return matchedEndpoints;
		}

	}

}
