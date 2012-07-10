package org.ow2.chameleon.rose.pubsubhubbub.topic.connector;

import org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HubMode;

public interface SubscriberConnector {
	boolean checkPending(String topicUrl, HubMode hubMode);
	void addHub(String hubUrl);
	void removeHub(String hubUrl);
	boolean connect(String topicUrl);
	void unconnect(String topicUrl);
	

}
