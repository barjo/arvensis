package org.ow2.chameleon.rose.pubsubhubbub.topic.publisher;

public interface Publishing {
	
	String COMPONENT_NAME="Publishing";

	void bindManager(PublisherManager manager);

	String getTopic();

	public void addFeed(String title, String content );
}
