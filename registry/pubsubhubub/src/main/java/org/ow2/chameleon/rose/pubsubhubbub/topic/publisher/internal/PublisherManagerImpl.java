package org.ow2.chameleon.rose.pubsubhubbub.topic.publisher.internal;

import static org.ow2.chameleon.rose.pubsubhubbub.topic.publisher.PublisherManager.COMPONENT_NAME;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.rose.pubsubhubbub.topic.connector.HubConnector;
import org.ow2.chameleon.rose.pubsubhubbub.topic.connector.PublisherConnector;
import org.ow2.chameleon.rose.pubsubhubbub.topic.publisher.PublisherManager;
import org.ow2.chameleon.rose.pubsubhubbub.topic.publisher.Publishing;

@Component(name=COMPONENT_NAME)
public class PublisherManagerImpl implements PublisherManager,
		ServiceTrackerCustomizer {

	@Property(name="hub.url")
	private String hubUrl;
	
	private BundleContext context;
	private ServiceTracker tracker;
	private PublisherConnector hubConnect;

	@Requires
	private LogService log;

	public PublisherManagerImpl(BundleContext context) {
		super();
		this.context = context;
	}

	@Validate
	void start() {

		tracker = new ServiceTracker(context, Publishing.class.getName(), this);
		tracker.open();
		
		hubConnect = HubConnector.getPublisherConnector(hubUrl);	
	}

	@Invalidate
	void stop() {
		tracker.close();
	}

	public void hubNotify(String topicUrl) {
		hubConnect.update(topicUrl);
	}

	public void addHub(String hubUrl) {
		hubConnect.addHub(hubUrl);

	}

	public void removeHub(String hubUrl) {
		hubConnect.removeHub(hubUrl);

	}

	public Object addingService(ServiceReference reference) {
		Publishing publisher = (Publishing) context.getService(reference);

		// bind
		publisher.bindManager(this);
		
		return publisher;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
	}

}
