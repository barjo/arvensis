package org.ow2.chameleon.rose.pubsubhubbub.distributedhub.internal;

import static org.osgi.service.log.LogService.LOG_INFO;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.ow2.chameleon.rose.pubsubhubbub.constants.PubsubhubbubConstants.HTTP_POST_PARAMETER_RECONNECT;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_LINK_HUBURL;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_PARAMETER_CALLBACKURL;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_PARAMETER_ENDPOINT;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_PARAMETER_PUBLISHER;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_PARAMETER_SUBSCRIBER;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.ws.rs.core.MediaType;

import org.osgi.service.log.LogService;
import org.ow2.chameleon.rose.pubsubhubbub.distributedhub.HubBackups;
import org.ow2.chameleon.rose.pubsubhubbub.hub.Hub;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.representation.Form;

public class ClientJersey {

	private static final Integer THREADPOOL_SIZE = 10;

	private Client client;
	private LogService logger;
	private String machineID;
	private String resourceURI;
	private ExecutorService executor;
	private HubBackups hubBackup;
	private Hub hub;
	private JerseyDistributedHub distributedHub;

	public ClientJersey(LogService pLogger, String pMachineID,
			HubBackups connectedHubs, Hub pHub,
			JerseyDistributedHub jerseyDistributedHub) {

		machineID = pMachineID;
		logger = pLogger;
		hubBackup = connectedHubs;
		hub = pHub;
		distributedHub = jerseyDistributedHub;

		ClientConfig cc = new DefaultClientConfig();
		cc.getProperties().put(DefaultClientConfig.PROPERTY_THREADPOOL_SIZE,
				THREADPOOL_SIZE);
		client = Client.create(cc);

		executor = Executors.newFixedThreadPool(THREADPOOL_SIZE);
	}

	public final void removeEndpoint(long endpointID,
			String publisherMachineID, Collection<String> connectedHubs) {

		executor.execute(new SendMultipleNotifications(
				JerseySendOptions.removeEndpoint, null, endpointID,
				publisherMachineID, null, null, connectedHubs));
	}

	public final void addEndpoint(String endpoint, String publisherMachineID,
			Collection<String> connectedHubs) {
		
		executor.execute(new SendMultipleNotifications(
				JerseySendOptions.newEndpoint, endpoint, 0, publisherMachineID,
				null, null, connectedHubs));

	}

	public void addSubscriber(String subscriberMachineID, String callBackUrl,
			Set<String> connectedHubs) {
		executor.execute(new SendMultipleNotifications(
				JerseySendOptions.newSubscriber, null, 0, null,
				subscriberMachineID, callBackUrl, connectedHubs));
	}

	public void removeSubscriber(String pMachineID, Set<String> connectedHubs) {
		executor.execute(new SendMultipleNotifications(
				JerseySendOptions.removeSubscriber, null, 0, null, pMachineID,
				null, connectedHubs));
	}

	public void addPublisher(String publisherMachineID, String callBackUrl,
			Set<String> connectedHubs) {
		executor.execute(new SendMultipleNotifications(
				JerseySendOptions.newPublisher, null, 0, null,
				publisherMachineID, callBackUrl, connectedHubs));

	}

	public void removePublisher(String publisherMachineID,
			Set<String> connectedHubs) {
		executor.execute(new SendMultipleNotifications(
				JerseySendOptions.removePublisher, null, 0, machineID, null,
				null, connectedHubs));
	}

	public final String linkToHub(String bootstrapHub, String jerseyHubUri,
			String jsonEndpoints, String jsonSubscribers, String jsonPublishers) {
		ClientResponse response;
		resourceURI = bootstrapHub + "/" + machineID + "/link";
		try {
			WebResource wr = client.resource(resourceURI);
			Form f = new Form();
			f.add(JERSEY_POST_LINK_HUBURL, jerseyHubUri);
			// set parameter only if exists any endpoints
			if (jsonEndpoints != null) {
				f.add(JERSEY_POST_PARAMETER_ENDPOINT, jsonEndpoints);
			}

			// set parameter only if exists any subscribers
			if (jsonSubscribers != null) {
				f.add(JERSEY_POST_PARAMETER_SUBSCRIBER, jsonSubscribers);
			}

			// set parameter only if exists any publishers
			if (jsonPublishers != null) {
				f.add(JERSEY_POST_PARAMETER_PUBLISHER, jsonPublishers);
			}

			response = wr.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
					.post(ClientResponse.class, f);

			// check response status
			if (response.getStatus() == Status.OK.getStatusCode()) {
				return response.getEntity(String.class);
			} else {
				throw new Exception("Error on server side");
			}

		} catch (Exception e) {
			logger.log(LOG_WARNING, "Problem with connection to " + resourceURI
					+ "  no endpoint retrieved", e);
			return null;
		}

	}

	public void unlink(Set<String> connectedHubs) {
		ClientResponse response;
		for (String hubUrl : connectedHubs) {
			resourceURI = hubUrl + "/" + machineID + "/unlink";
			WebResource wr = client.resource(resourceURI);
			response = wr.delete(ClientResponse.class);

			// check response status
			if (response.getStatus() == Status.OK.getStatusCode()) {
				logger.log(LOG_INFO, "Successfully unlink to " + resourceURI);
			} else {
				logger.log(LOG_WARNING, "Problem with unlink to " + resourceURI);
			}

		}

	}

	public String retrieveMachineID(String linkURI) {
		resourceURI = linkURI + "/" + machineID + "/machineID";
		try {
			WebResource wr = client.resource(resourceURI);
			return wr.get(String.class);
		} catch (Exception e) {
			logger.log(LOG_WARNING,
					"Problem with connection to " + resourceURI, e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private enum JerseySendOptions {
		newEndpoint, removeEndpoint, newSubscriber, removeSubscriber, newPublisher, removePublisher
	}

	private final class SendMultipleNotifications extends Thread {
		private Collection<String> connectedHubs;
		private JerseySendOptions option;
		private String endpoint;
		private long endpointID;
		private String publisherMachineID;
		private WebResource wr;
		private Form f;
		private ClientResponse response;
		private String subscriberMachineID;
		private String callBack;
		private Client client;

		/**
		 * @param pOption
		 * @param pEndpoint
		 * @param pEndpointID
		 * @param pPublisherMachineID
		 * @param pSubscriberMachineID
		 * @param pCallBack
		 * @param pConnectedHubs
		 */
		private SendMultipleNotifications(JerseySendOptions pOption,
				String pEndpoint, long pEndpointID, String pPublisherMachineID,
				String pSubscriberMachineID, String pCallBack,
				Collection<String> pConnectedHubs) {
			super();
			this.endpoint = pEndpoint;
			this.connectedHubs = pConnectedHubs;
			this.option = pOption;
			this.endpointID = pEndpointID;
			this.publisherMachineID = pPublisherMachineID;
			this.subscriberMachineID = pSubscriberMachineID;
			this.callBack = pCallBack;

			ClientConfig cc = new DefaultClientConfig();
			cc.getProperties().put(
					DefaultClientConfig.PROPERTY_THREADPOOL_SIZE,
					THREADPOOL_SIZE);
			this.client = Client.create(cc);
		}

		@Override
		public void run() {
			if (JerseyDistributedHub.running) {
			f = new Form();

			for (String hubUrl : connectedHubs) {

				switch (option) {

				// new endpoint
				case newEndpoint:
					wr = client.resource(hubUrl + "/" + machineID
							+ "/endpoints/" + this.publisherMachineID);
					f.clear();
					f.add(JERSEY_POST_PARAMETER_ENDPOINT, this.endpoint);
					response = wr.type(
							MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(
							ClientResponse.class, f);
					break;

				// removeEndpoint
				case removeEndpoint:
					wr = client.resource(hubUrl + "/" + machineID
							+ "/endpoints/" + this.publisherMachineID + "/"
							+ endpointID);
					response = wr.delete(ClientResponse.class);
					break;

				// new Subscriber
				case newSubscriber:
					f.clear();
					f.add(JERSEY_POST_PARAMETER_SUBSCRIBER,
							this.subscriberMachineID);
					f.add(JERSEY_POST_PARAMETER_CALLBACKURL, this.callBack);
					wr = client.resource(hubUrl + "/" + machineID
							+ "/subscriber");
					response = wr.type(
							MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(
							ClientResponse.class, f);
					break;

				case removeSubscriber:
					wr = client.resource(hubUrl + "/" + machineID
							+ "/subscriber/" + this.subscriberMachineID);
					response = wr.delete(ClientResponse.class);
					break;

				case newPublisher:
					f.clear();
					f.add(JERSEY_POST_PARAMETER_PUBLISHER,
							this.publisherMachineID);
					f.add(JERSEY_POST_PARAMETER_CALLBACKURL, this.callBack);
					wr = client.resource(hubUrl + "/" + machineID
							+ "/publisher");
					response = wr.type(
							MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(
							ClientResponse.class, f);
					break;
				case removePublisher:
					wr = client.resource(hubUrl + "/" + machineID
							+ "/publisher/" + this.publisherMachineID);
					response = wr.delete(ClientResponse.class);
					break;

				default:
					break;
				}

				// check reponse status
				if (response.getStatus() == Status.OK.getStatusCode()) {
					logger.log(LOG_INFO,
							"Successfully send" + option.toString() + " to: "
									+ hubUrl);
				}
				// pubsubhubbub is down, reconnect to backup
				// subscribers/publishers
				else if (response.getStatus() == Status.NOT_FOUND
						.getStatusCode()) {
					executor.execute(new Reconnect(hubUrl));
				} else {
					logger.log(LOG_WARNING,
							"Unsuccessfully send" + option.toString() + " to: "
									+ hubUrl);
				}

			}

		}
		}
	}

	private class Reconnect extends Thread {
		private String noRespondingHub;
		private WebResource wr;
		private Client client;
		private Form f;
		private ClientResponse response;
		private Map<String, String> publishers;
		private Map<String, String> subscribers;

		public Reconnect(String hubUrl) {
			super();
			noRespondingHub = hubUrl;
			ClientConfig cc = new DefaultClientConfig();
			cc.getProperties().put(
					DefaultClientConfig.PROPERTY_THREADPOOL_SIZE,
					THREADPOOL_SIZE);
			this.client = Client.create(cc);

			f = new Form();
		}

		@Override
		public void run() {
			if (JerseyDistributedHub.running){
			publishers = hubBackup.getPublishers(noRespondingHub);
			subscribers = hubBackup.getSubscribers(noRespondingHub);

			// remove hub from connected
			hubBackup.removeConnectedHubByURI(noRespondingHub);

			// connect to publishers and register them on this Pubsubhubbub
			for (Entry<String, String> publisher : publishers.entrySet()) {
				wr = client.resource(publisher.getValue());
				f.clear();
				f.add(HTTP_POST_PARAMETER_RECONNECT, hub.getUrl());
				response = wr.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
						.post(ClientResponse.class, f);

				if (response.getStatus() == Status.OK.getStatusCode()) {
					// register new publisher
					hub.getRegistrations().addTopic(
							response.getEntity(String.class),
							publisher.getKey(), publisher.getValue());
					// notify other hubs
					distributedHub.addBackupPublisher(publisher.getKey(),
							publisher.getValue());
					logger.log(
							LOG_INFO,
							"Successfully changed new Pubsubhubbub url: "
									+ hub.getUrl() + ", in publisher"
									+ publisher.getValue());
				} else {
					logger.log(LOG_WARNING,
							"Unsuccessfully changed new Pubsubhubbub url: "
									+ hub.getUrl() + ", in publisher"
									+ publisher.getValue());
				}
			}
			// connect to publishers and register them on this Pubsubhubbub
			for (Entry<String, String> subscriber : subscribers.entrySet()) {
				wr = client.resource(subscriber.getValue());
				f.clear();
				f.add(HTTP_POST_PARAMETER_RECONNECT, hub.getUrl());
				response = wr.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
						.post(ClientResponse.class, f);
				if (response.getStatus() == Status.OK.getStatusCode()) {
					// register new subscriber
					hub.getRegistrations().addSubscriber(subscriber.getValue(),
							response.getEntity(String.class),
							subscriber.getKey());
					// notify other hubs;
					distributedHub.addBackupSubscriber(subscriber.getKey(),
							subscriber.getValue());
					logger.log(
							LOG_INFO,
							"Successfully changed new Pubsubhubbub url: "
									+ hub.getUrl() + ", in subsciber"
									+ subscriber.getValue());
				} else {
					logger.log(LOG_INFO,
							"Unsuccessfully changed new Pubsubhubbub url: "
									+ hub.getUrl() + ", in subscriber"
									+ subscriber.getValue());
				}

			}
		}
	}
	}
}
