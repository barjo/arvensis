package org.ow2.chameleon.rose.pubsubhubbub.distributedhub.internal;

import static org.osgi.service.log.LogService.LOG_INFO;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_LINK_HUBURL;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_PARAMETER_NEW_ENDPOINT;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_SERVLET_ALIAS;

import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.osgi.service.log.LogService;

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

	public ClientJersey(LogService pLogger) {

		ClientConfig cc = new DefaultClientConfig();
		cc.getProperties().put(DefaultClientConfig.PROPERTY_THREADPOOL_SIZE,
				THREADPOOL_SIZE);
		client = Client.create(cc);
		logger=pLogger;
	}

	public final void removeEndpoint(long endpointID, String machineID,
			Set<String> connectedHubs) {

		(new SendJersey(JerseySendOptions.removeEndpoint, endpointID,
				machineID, connectedHubs)).start();

	}

	public final void addEndpoint(String endpoint, String machineID,
			Set<String> connectedHubs) {

		(new SendJersey(JerseySendOptions.newEndpoint, endpoint, machineID,
				connectedHubs)).start();

	}

	public final String retrieveEndpoints(String bootstrapHub, String jerseyHubUri) {
		try {
			WebResource wr = client.resource(bootstrapHub
					+ JERSEY_SERVLET_ALIAS + "/link/");
			Form f = new Form();
			f.add(JERSEY_POST_LINK_HUBURL, jerseyHubUri);
			return wr.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(
					String.class, f);
		} catch (Exception e) {
			logger.log(LOG_WARNING, "Problem with connection to "
					+ bootstrapHub + JERSEY_SERVLET_ALIAS + "/link/"
					+ "  no endpoint retrieved", e);
			return null;
		}

	}

	private enum JerseySendOptions {
		newEndpoint, removeEndpoint
	}

	private final class SendJersey extends Thread {
		private Set<String> connectedHubs;
		private JerseySendOptions option;
		private String endpoint;
		private long endpointID;
		private String machineID;
		private WebResource wr;
		private Form f;
		private ClientResponse response;

		private SendJersey(JerseySendOptions pNewEndponit, String pEndpoint,
				String pMachineID, Set<String> pConnectedHubs) {
			this.connectedHubs = pConnectedHubs;
			this.option = pNewEndponit;
			this.endpoint = pEndpoint;
			this.machineID = pMachineID;
		}

		private SendJersey(JerseySendOptions pNewEndponit, long pEndpointID,
				String pMachineID, Set<String> pConnectedHubs) {
			this.connectedHubs = pConnectedHubs;
			this.option = pNewEndponit;
			this.endpointID = pEndpointID;
			this.machineID = pMachineID;
		}

		@Override
		public void run() {
			f = new Form();
			
			switch (option) {
			case newEndpoint:
				for (String hubUrl : connectedHubs) {
					wr = client.resource(hubUrl + "/endpoints/" + machineID);
					f.clear();
					f.add(JERSEY_POST_PARAMETER_NEW_ENDPOINT, endpoint);
					response = wr.type(
							MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(
							ClientResponse.class, f);
					if (response.getStatus() == Status.OK.getStatusCode()) {
						logger.log(LOG_INFO,
								"Successfully send new Endpoint notification (publisher: "
										+ machineID + ")to: " + hubUrl);
					} else {
						logger.log(LOG_WARNING,
								"Unsuccessfully send new Endpoint notification (publisher: "
										+ machineID + ")to: " + hubUrl);
					}
				}
				break;
			case removeEndpoint:
				for (String hubUrl : connectedHubs) {
					wr = client.resource(hubUrl + "/endpoints/" + machineID
							+ "/" + endpointID);
					response = wr.type(
							MediaType.APPLICATION_FORM_URLENCODED_TYPE).delete(
							ClientResponse.class);
					if (response.getStatus() == Status.OK.getStatusCode()) {
						logger.log(LOG_INFO,
								"Successfully send remove Endpoint number "
										+ endpoint + ", publisher: "
										+ machineID + ", notification to: "
										+ hubUrl);
					} else {
						logger.log(LOG_WARNING,
								"Unsuccessfully send remove Endpoint number "
										+ endpoint + ", publisher: "
										+ machineID + ", notification to: "
										+ hubUrl);
					}
				}
				break;
			default:
				break;
			}
			

		}

	}

}
