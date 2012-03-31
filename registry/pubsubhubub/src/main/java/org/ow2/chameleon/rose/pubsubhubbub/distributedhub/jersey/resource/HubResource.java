package org.ow2.chameleon.rose.pubsubhubbub.distributedhub.jersey.resource;

import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_LINK_HUBURL;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_PARAMETER_CALLBACKURL;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_PARAMETER_ENDPOINT;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_PARAMETER_SUBSCRIBER;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_PARAMETER_PUBLISHER;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub;
import org.ow2.chameleon.rose.pubsubhubbub.hub.Hub;

@Path("/")
public class HubResource {

	@Context
	private JSONService json;

	@Context
	private Hub hub;

	@Context
	private DistributedHub distributedHub;

	@GET
	@Path("{requestMachineID}/machineID")
	@Produces(MediaType.TEXT_PLAIN)
	public final String getMachineID() {
		return distributedHub.getMachineID();
	}

	@GET
	@Path("{requestMachineID}/endpoints")
	@Produces(MediaType.APPLICATION_JSON)
	public final String getAllEndpoints() {
		return json.toJSON(hub.getRegistrations().getAllEndpoints());
	}


	/**Save connection to given hub
	 * @param hubLink Hub URI 
	 * @param jsonEndpoints list of endpoints to add
	 * @param jsonSubscribers list of backup subscribers to add
	 * @param jsonPublishers list of backup publishers  to add
	 * @param machineID distributed hub machineID to register
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@POST
	@Path("{requestMachineID}/link")
	@Produces(MediaType.APPLICATION_JSON)
	public final Response linkToHub(
			@FormParam(JERSEY_POST_LINK_HUBURL) String hubLink,
			@FormParam(JERSEY_POST_PARAMETER_ENDPOINT) String jsonEndpoints,
			@FormParam(JERSEY_POST_PARAMETER_SUBSCRIBER) String jsonSubscribers,
			@FormParam(JERSEY_POST_PARAMETER_PUBLISHER) String jsonPublishers,
			@PathParam("requestMachineID") String machineID) {

		Map<String, String> returnMap = new HashMap<String, String>();

		if (hubLink == null) {
			return Response.notModified().build();
		}

		// register new endpoints if any
		if (jsonEndpoints != null) {
			try {
				for (Entry<String, String> entry : ((Map<String, String>) json
						.fromJSON(jsonEndpoints)).entrySet()) {
					hub.getRegistrations().addEndpointByMachineID(
							entry.getValue(),
							hub.getEndpointDescriptionFromJSON(json
									.fromJSON(entry.getKey())));
				}
			} catch (ParseException e) {
				e.printStackTrace();
				return Response.notModified().build();
			}
		}

		// add connection to hub
		distributedHub.getHubBackups().addConnectedHub(machineID, hubLink);

		// backup subscribers
		if (jsonSubscribers != null) {
			try {
				for (Entry<String, String> entry : ((Map<String, String>) json
						.fromJSON(jsonSubscribers)).entrySet()) {
					distributedHub.getHubBackups().addSubscriber(machineID,
							entry.getValue(), entry.getKey());
				}
			} catch (ParseException e) {
				e.printStackTrace();
				return Response.notModified().build();
			}
		}

		// backup publishers
		if (jsonPublishers != null) {
			try {
				for (Entry<String, String> entry : ((Map<String, String>) json
						.fromJSON(jsonPublishers)).entrySet()) {
					distributedHub.getHubBackups().addPublisher(machineID,
							entry.getValue(), entry.getKey());
				}
			} catch (ParseException e) {
				e.printStackTrace();
				return Response.notModified().build();
			}
		}

		Map<String, String> allEndpoinsJSON = new HashMap<String, String>();
		// send registered endpoints in JSON
		for (Entry<EndpointDescription, String> entry : hub.getRegistrations()
				.getAllEndpoints().entrySet()) {
			allEndpoinsJSON.put(json.toJSON(entry.getKey().getProperties()),
					entry.getValue());

		}
		if (allEndpoinsJSON.size() != 0) {
			// put endpoints if any to return JSON map
			returnMap.put(JERSEY_POST_PARAMETER_ENDPOINT,
					json.toJSON(allEndpoinsJSON));
		}

		if (hub.getRegistrations().getSubscribers().size() != 0) {
			// put subscribers if any to return JSON map
			returnMap.put(JERSEY_POST_PARAMETER_SUBSCRIBER,
					json.toJSON(hub.getRegistrations().getSubscribers()));
		}
		if (hub.getRegistrations().getPublishers().size() != 0) {
			// put publishers if any to return JSON map
			returnMap.put(JERSEY_POST_PARAMETER_PUBLISHER,
					json.toJSON(hub.getRegistrations().getPublishers()));
		}

		return Response.ok(json.toJSON(returnMap)).build();
	}

	/**
	 * Remove connection to Distributed hub
	 * 
	 * @param machineID
	 *            distributed hub to remove
	 * @return
	 */
	@DELETE
	@Path("{requestMachineID}/unlink")
	@Produces(MediaType.APPLICATION_JSON)
	public final Response uninkToHub(
			@PathParam("requestMachineID") String machineID) {

		distributedHub.getHubBackups().removeConnectedHubByID(machineID);
		return Response.ok().build();
	}

	@GET
	@Path("{requestMachineID}/endpoints/{publisher}")
	@Produces(MediaType.APPLICATION_JSON)
	public final String getPublisherEndpoints(
			@PathParam("publisher") String publisher) {
		Map<EndpointDescription, String> endpointPublisher = new HashMap<EndpointDescription, String>();
		for (Entry<EndpointDescription, String> entry : hub.getRegistrations()
				.getAllEndpoints().entrySet()) {
			if (entry.getValue().equals(publisher)) {
				endpointPublisher.put(entry.getKey(), entry.getValue());
			}
		}
		return json.toJSON(endpointPublisher);
	}

	@SuppressWarnings("unchecked")
	@POST
	@Path("{requestMachineID}/endpoints/{publisher}")
	public final Response newEndpoint(
			@FormParam(JERSEY_POST_PARAMETER_ENDPOINT) String endp,
			@PathParam("publisher") String publisher,
			@PathParam("requestMachineID") String machineID) {
		EndpointDescription endpoint;
		try {
			endpoint = hub.getEndpointDescriptionFromJSON(json.fromJSON(endp));
			// if endpoints already registered, don`t notify other hubs
			// (prevents looping)
			if (hub.getRegistrations().addEndpointByMachineID(publisher,
					endpoint)) {
				distributedHub.addEndpoint(endpoint, publisher, machineID);
			}
		} catch (ParseException e) {
			Response.notModified().build();
		}
		return Response.ok().build();
	}

	@DELETE
	@Path("{requestMachineID}/endpoints/{publisher}/{id}")
	public final Response removeEndpoint(@PathParam("id") long endpointId,
			@PathParam("publisher") String publisher,
			@PathParam("requestMachineID") String machineID) {
		// if endpoints already registered, don`t notify other hubs (prevents
		// looping)
		if (hub.getRegistrations().removeEndpoint(publisher, endpointId)) {
			distributedHub.removeEndpoint(endpointId, publisher, machineID);
		}
		return Response.ok().build();
	}

	@POST
	@Path("{requestMachineID}/subscriber")
	public final Response newSubscriber(
			@FormParam(JERSEY_POST_PARAMETER_SUBSCRIBER) String subscriberMachineID,
			@FormParam(JERSEY_POST_PARAMETER_CALLBACKURL) String subscriberCallBackURL,
			@PathParam("requestMachineID") String machineID) {

		distributedHub.getHubBackups().addSubscriber(machineID,
				subscriberMachineID, subscriberCallBackURL);
		return Response.ok().build();
	}

	@DELETE
	@Path("{requestMachineID}/subscriber/{subscriberMachineID}")
	public final Response removeSubscriber(
			@PathParam("subscriberMachineID") String subscriberMachineID,
			@PathParam("requestMachineID") String machineID) {

		distributedHub.getHubBackups().removeSubscriber(machineID,
				subscriberMachineID);
		return Response.ok().build();
	}

	@POST
	@Path("{requestMachineID}/publisher")
	public final Response newPublisher(
			@FormParam(JERSEY_POST_PARAMETER_PUBLISHER) String publisherMachineID,
			@FormParam(JERSEY_POST_PARAMETER_CALLBACKURL) String publisherCallBackURL,
			@PathParam("requestMachineID") String machineID) {

		distributedHub.getHubBackups().addPublisher(machineID,
				publisherMachineID, publisherCallBackURL);
		return Response.ok().build();
	}

	@DELETE
	@Path("{requestMachineID}/publisher/{publisherMachineID}")
	public final Response removePublisher(
			@PathParam("publisherMachineID") String publisherMachineID,
			@PathParam("requestMachineID") String machineID) {

		distributedHub.getHubBackups().removeSubscriber(machineID,
				publisherMachineID);
		return Response.ok().build();
	}

}
