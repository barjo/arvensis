package org.ow2.chameleon.rose.pubsubhubbub.topic.publisher;

public interface PublisherManager {

	String COMPONENT_NAME="PublisherManager";
	
	void hubNotify(String topicUrl);
	void addHub(String hubUrl);
	void removeHub(String hubUrl);
	
}
