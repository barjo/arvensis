package org.ow2.chameleon.rose.constants;

import java.util.Map;

public class RoseRSSConstants {

	public static final String RSS_EVENT_TOPIC = "org/ow2/chameleon/syndication";
	public static final String FEED_TITLE_NEW = "Endpoint added";
	public static final String FEED_TITLE_REMOVE = "Endpoint removed";
	public static final String FEED_AUTHOR = "Rose RSS";
	public static final String HTTP_POST_HEADER_TYPE="application/x-www-form-urlencoded";
	public static final String HTTP_POST_PARAMETER_HUB_MODE="hub.mode";
	public static final String HTTP_POST_PARAMETER_URL_CALLBACK = "hub.callback";
	public static final String HTTP_POST_PARAMETER_RSS_TOPIC_URL="hub.topic";
	public static final String HTTP_POST_PARAMETER_ENDPOINT_FILTER="hub.endp.filter";
	public static final String HUB_UPDATE_ENDPOINT_ADDED = "endpoint.add";
	public static final String HUB_UPDATE_ENDPOINT_REMOVED = "endpoint.remove";
	
	public enum HubMode {
		publish, unpublish, update, subscribe, unsubscribe, getAllEndpoints;

		public Object getValue(Map<String, Object> values) {
			return values.get(this.toString());
		}
	}
}
