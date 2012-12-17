package org.ow2.chameleon.rose.pubsubhubbub.distributedhub.internal;

import static org.osgi.service.log.LogService.LOG_INFO;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.COMPONENT_NAME;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.ws.rs.core.Context;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub;
import org.ow2.chameleon.rose.pubsubhubbub.distributedhub.HubBackups;
import org.ow2.chameleon.rose.pubsubhubbub.hub.Hub;
import org.ow2.chameleon.rose.util.DefaultLogService;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.inject.SingletonTypeInjectableProvider;

@Component(name = COMPONENT_NAME)
@Provides
public class JerseyDistributedHub implements DistributedHub {

	@Requires
	private HttpService httpService;

	@Requires
	private JSONService json;

	@Requires(id = "hubID")
	private Hub hub;

	@Requires(id = "roseID")
	private RoseMachine rose;

	private BundleContext context;

//	@Requires(optional = true, defaultimplementation = DefaultLogService.class)
	private LogService logger = new DefaultLogService();

	@Property(name = BOOTSTRAP_LINK_INSTANCE_PROPERTY, mandatory = false)
	private String bootstrapHubLink;

	@Property(name = JERSEY_ALIAS_INSTANCE_PROPERTY, mandatory = false, value = JERSEY_DEFAULT_SERVLET_ALIAS)
	private String jerseyServletAlias;

	private ServletContainer servletContainer;
	private ClientJersey clientJersey;

	// connected hubs with info of their subscribers and publishers
	private HubBackups connectedHubs;

	private String jerseyHubUri;
	
	protected static volatile Boolean running = false; //running flag

	public JerseyDistributedHub(BundleContext pContext) {
		this.context = pContext;
	}

	@SuppressWarnings("unused")
	@Validate
	private void start() {
		String port = null;
		try {
			connectedHubs = new HubBackupsImpl();

			// retrieve an ip address and port of gateway
			final ServiceReference httpServiceRef = context
					.getServiceReference(HttpService.class.getName());
			if (httpServiceRef != null) {
				port = (String) httpServiceRef
						.getProperty("org.osgi.service.http.port");

			}
			if (port == null) {
				port = context.getProperty("org.osgi.service.http.port");
			}
			jerseyHubUri = "http://" + rose.getHost() + ":" + port
					+ jerseyServletAlias;

			clientJersey = new ClientJersey(logger, this.getMachineID(),
					connectedHubs, hub, this);

			// deploy resources
			deployRestResource();

			// connect and retrieve endpoints from other hub
			if (bootstrapHubLink != null) {
				this.establishConnection(bootstrapHubLink);
			}
			logger.log(LOG_INFO,
					"Distributed Pubsubhubbub successfully started");
			running = true; 
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unused")
	@Invalidate
	private void stop() {
		running = false;
		logger.log(LOG_INFO, "Distributed Pubsubhubbub stopping");
		httpService.unregister(jerseyServletAlias);
		servletContainer.destroy();
		clientJersey.unlink(connectedHubs.getAllHubsLink());

	}

	/**
	 * Deploy Pubsubhubbub REST resources on application server.
	 * 
	 * @throws NamespaceException
	 * @throws ServletException
	 */
	private void deployRestResource() throws NamespaceException,
			ServletException {

		PackagesResourceConfig pckgc = new PackagesResourceConfig(
				"org.ow2.chameleon.rose.pubsubhubbub.distributedhub.jersey.resource");
		// inject json and hub to resource
		pckgc.getSingletons().add(
				new SingletonTypeInjectableProvider<Context, JSONService>(
						JSONService.class, json) {
				});
		pckgc.getSingletons().add(
				new SingletonTypeInjectableProvider<Context, Hub>(Hub.class,
						hub) {
				});
		// inject distributedhub
		pckgc.getSingletons().add(
				new SingletonTypeInjectableProvider<Context, DistributedHub>(
						DistributedHub.class, this) {
				});
		servletContainer = new ServletContainer(pckgc);
		// register jersey resource as servlet
		httpService.registerServlet(jerseyServletAlias, servletContainer, null,
				null);

	}

	public final void addEndpoint(EndpointDescription endpoint,
			String publisherMachineID) {
		clientJersey.addEndpoint(json.toJSON(endpoint.getProperties()),
				publisherMachineID, connectedHubs.getAllHubsLink());

	}

	public final void addEndpoint(EndpointDescription endpoint,
			String publisherMachineID, String excludeMachineID) {

		clientJersey.addEndpoint(json.toJSON(endpoint.getProperties()),
				publisherMachineID,
				connectedHubs.getAllHubsLink(excludeMachineID));

	}

	public final void removeEndpoint(long endpointID, String machineID) {
		clientJersey.removeEndpoint(endpointID, machineID,
				connectedHubs.getAllHubsLink());

	}

	public final void removeEndpoint(long endpointID, String machineID,
			String excludeMachineID) {
		clientJersey.removeEndpoint(endpointID, machineID,
				connectedHubs.getAllHubsLink(excludeMachineID));

	}

	public final String getHubUri() {
		return jerseyHubUri;
	}

	@SuppressWarnings("unchecked")
	public void establishConnection(String linkURI) throws ParseException {
		String jsonEndpoints = null;
		String jsonSubscribers = null;
		String jsonPublishers = null;
		String machineID;

		// get all registers endpoints
		Map<EndpointDescription, String> endpoints = hub.getRegistrations()
				.getAllEndpoints();

		// check if any registered
		if (endpoints.size() != 0) {
			// change every endpoint to JSON properties
			Map<String, String> jsonMap = new HashMap<String, String>();
			for (Entry<EndpointDescription, String> element : endpoints
					.entrySet()) {
				jsonMap.put(json.toJSON(element.getKey().getProperties()),
						element.getValue());
			}
			jsonEndpoints = json.toJSON(jsonMap);
		}

		// check if already registered subscribers
		if (!hub.getRegistrations().getSubscribers().isEmpty()) {
			jsonSubscribers = json.toJSON(hub.getRegistrations()
					.getSubscribers());
		}

		// check if already registered publishers
		if (!hub.getRegistrations().getPublishers().isEmpty()) {
			jsonPublishers = json
					.toJSON(hub.getRegistrations().getPublishers());
		}

		machineID = clientJersey.retrieveMachineID(linkURI);

		// save linked hub
		connectedHubs.addConnectedHub(machineID, linkURI);

		// parse a response, iterate on endpoints/subscribers received from link
		// hub
		for (Entry<String, String> rootEntry : ((Map<String, String>) json
				.fromJSON(clientJersey.linkToHub(linkURI, this.jerseyHubUri,
						jsonEndpoints, jsonSubscribers, jsonPublishers)))
				.entrySet()) {

			// endpoints
			if (rootEntry.getKey().equals(JERSEY_POST_PARAMETER_ENDPOINT)) {

				for (Entry<String, String> endpointEntry : ((Map<String, String>) json
						.fromJSON(rootEntry.getValue())).entrySet()) {
					hub.getRegistrations().addEndpointByMachineID(
							endpointEntry.getValue(),
							hub.getEndpointDescriptionFromJSON(json
									.fromJSON(endpointEntry.getKey())));
				}
			}
			// susbcribers
			else if (rootEntry.getKey()
					.equals(JERSEY_POST_PARAMETER_SUBSCRIBER)) {
				for (Entry<String, String> subscriberEntry : ((Map<String, String>) json
						.fromJSON(rootEntry.getValue())).entrySet()) {
					connectedHubs.addSubscriber(machineID,
							subscriberEntry.getValue(),
							subscriberEntry.getKey());
				}
			}

			else if (rootEntry.getKey().equals(JERSEY_POST_PARAMETER_PUBLISHER)) {
				for (Entry<String, String> publisherEntry : ((Map<String, String>) json
						.fromJSON(rootEntry.getValue())).entrySet()) {
					connectedHubs.addPublisher(machineID,
							publisherEntry.getValue(), publisherEntry.getKey());
				}

			}
		}

	}

	public String getMachineID() {
		return rose.getId();
	}

	public void addBackupSubscriber(String subscriberMachineID,
			String callBackUrl) {

		clientJersey.addSubscriber(subscriberMachineID, callBackUrl,
				connectedHubs.getAllHubsLink());

	}

	public void removeBackupSubscriber(String subscriberMachineID) {
		clientJersey.removeSubscriber(subscriberMachineID,
				connectedHubs.getAllHubsLink());
	}

	public HubBackups getHubBackups() {
		return connectedHubs;
	}

	public void addBackupPublisher(String publisherMachineID, String callBackUrl) {
		clientJersey.addPublisher(publisherMachineID, callBackUrl,
				connectedHubs.getAllHubsLink());

	}

	public void removeBackupPublisher(String publisherMachineID) {
		clientJersey.removePublisher(publisherMachineID,
				connectedHubs.getAllHubsLink());

	}

}
