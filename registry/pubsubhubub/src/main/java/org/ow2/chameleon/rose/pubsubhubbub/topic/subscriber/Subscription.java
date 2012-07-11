package org.ow2.chameleon.rose.pubsubhubbub.topic.subscriber;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public interface Subscription {
	
	String COMPONENT_NAME ="Subscription";

	String getTopicUrl();
	void onContent(SyndEntry syndEntry);
}
