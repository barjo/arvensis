package org.ow2.chameleon.rose.pubsubhubbub.topic.subscriber.internal;

import static org.ow2.chameleon.rose.pubsubhubbub.topic.subscriber.Subscription.COMPONENT_NAME;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.ow2.chameleon.rose.pubsubhubbub.topic.subscriber.Subscription;

import com.sun.syndication.feed.synd.SyndEntry;

@Component(name=COMPONENT_NAME)
@Provides
public class SubscriptionImpl implements Subscription {

	@Property(name="topic.url",mandatory=true)
	private String topicUrl;
	
	public String getTopicUrl() {
		return topicUrl;
	}

	public void onContent(SyndEntry feed) {
		// TODO do something with the feed
		System.out.println("description: " +feed.getDescription().getValue());
		System.out.println("title: " +feed.getTitle());
		System.out.println("date: "+feed.getPublishedDate());

	}

}
