package org.ow2.chameleon.rose.pubsubhubbub.distributedhub.internal;

import static org.osgi.service.log.LogService.LOG_INFO;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.COMPONENT_NAME;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

	@Requires(id="hubID")
	private Hub hub;

	@Requires
	private RoseMachine rose;

	private BundleContext context;

	@Requires(optional = true, defaultimplementation = DefaultLogService.class)
	private  LogService logger;

	@Property(name = BOOTSTRAP_LINK_INSTANCE_PROPERTY, mandatory = false)
	private String bootstrapHubLink;
	
	@Property(name = JERSEY_SERVLET_INSTANCE_PROPERTY, mandatory = false , value=JERSEY_SERVLET_ALIAS)
	private String jerseyServletAlias;

	private ServletContainer servletContainer;
	private ClientJersey clientJersey;
	// stores links to other hubs
	private Set<String> connectedHubs;
	private String jerseyHubUri;

	public JerseyDistributedHub(BundleContext pContext) {
		this.context = pContext;
	}

	@SuppressWarnings({ "unused", "unchecked" })
	@Validate
	private void start() {
		String port = null; 
		try {
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
			connectedHubs = new HashSet<String>();
			clientJersey = new ClientJersey(logger);

			// deploy resources
			deployRestResource();
			// connect and retrieve endpoints from other hub
			if (bootstrapHubLink != null) {
				// save linked hub
				this.addConnectedHub(bootstrapHubLink);
				// iterate on endpoints received from link hub
				for (Entry<String, String> entry : ((Map<String, String>) (json
						.fromJSON(clientJersey.retrieveEndpoints(
								bootstrapHubLink, this.jerseyHubUri))))
						.entrySet()) {
					hub.getRegistrations().addEndpointByMachineID(
							entry.getValue(),
							hub.getEndpointDescriptionFromJSON(json
									.fromJSON(entry.getKey())));
				}

			}
			logger.log(LOG_INFO,
					"Distributed Pubsubhubbub successfully started");
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unused")
	@Invalidate
	private void stop() {
		httpService.unregister(JERSEY_SERVLET_ALIAS);
		servletContainer.destroy();
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
		httpService.registerServlet(jerseyServletAlias, servletContainer, null, null);

	}

	public final void addEndpoint(EndpointDescription endpoint, String machineID) {
		//TODO remove
		System.out.println(jerseyHubUri+" called add endpoint, sending to "+connectedHubs);
		clientJersey.addEndpoint(json.toJSON(endpoint.getProperties()),
				machineID, connectedHubs);

	}

	public final void removeEndpoint(long endpointID, String machineID) {
		clientJersey.removeEndpoint(endpointID, machineID, connectedHubs);

	}

	public final void addConnectedHub(String link) {
		connectedHubs.add(link);

	}

	public final void removeConnectedHub(String link) {
		connectedHubs.remove(link);

	}

	public final Set<String> getConnectedHubs() {
		return connectedHubs;
	}

	public final String getHubUri() {
		return jerseyHubUri;
	}

}
