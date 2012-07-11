package org.ow2.chameleon.rose.pubsubhubbub.topic.publisher.internal;

import static org.ow2.chameleon.rose.pubsubhubbub.topic.publisher.Publishing.COMPONENT_NAME;
import static org.osgi.service.log.LogService.LOG_INFO;

import javax.servlet.ServletException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.pubsubhubbub.topic.connector.HubConnector;
import org.ow2.chameleon.rose.pubsubhubbub.topic.publisher.PublisherManager;
import org.ow2.chameleon.rose.pubsubhubbub.topic.publisher.Publishing;

@Component(name = COMPONENT_NAME)
@Provides
public class PublishingImpl implements Publishing {

	@Requires
	private HttpService httpService;

	@Requires(optional = true)
	private LogService log;

	@Requires
	private RoseMachine rose;

	@Property(name = "topic.url", mandatory = true)
	private String topicURL;

	private PublisherManager manager;
	private RSSTopicManager topic;
	private BundleContext context;
	private String port;

	public PublishingImpl(BundleContext context) {
		super();
		this.context = context;
	}

	@Validate
	void start() throws ServletException, NamespaceException {
		port = HubConnector.findPort(context);

		topic = new RSSTopicManager(rose.getHost()+":"+port, topicURL, httpService,
				context);
		log.log(LOG_INFO, COMPONENT_NAME+ "has successfully started");
	}

	@Invalidate
	void stop() {
		topic.stop();
	}

	public void bindManager(PublisherManager manager) {
		this.manager = manager;

	}

	public String getTopic() {
		return topicURL;

	}

	public void addFeed(String title, String content) {

		topic.addEntry(title, content);
		
		//set full topic url address
		 manager.hubNotify("http://"+rose.getHost()+":"+port+topicURL);
	}

}
