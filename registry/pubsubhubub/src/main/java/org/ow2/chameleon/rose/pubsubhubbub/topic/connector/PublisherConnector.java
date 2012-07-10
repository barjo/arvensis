package org.ow2.chameleon.rose.pubsubhubbub.topic.connector;

public interface PublisherConnector {
	void addHub(String hubUrl);
	void removeHub(String hubUrl);
	void update(String topic);

}
