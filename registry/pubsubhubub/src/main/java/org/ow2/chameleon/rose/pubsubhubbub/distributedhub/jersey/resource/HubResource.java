package org.ow2.chameleon.rose.pubsubhubbub.distributedhub.jersey.resource;

import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_LINK_ENDPOINTS;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_LINK_HUBURL;
import static org.ow2.chameleon.rose.pubsubhubbub.distributedhub.DistributedHub.JERSEY_POST_PARAMETER_ENDPOINT;

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
	@Path("endpoints")
	@Produces(MediaType.APPLICATION_JSON)
	public final String getAllEndpoints() {
		return json.toJSON(hub.getRegistrations().getAllEndpoints());
	}

	/**
	 * Save connection to given hub
	 * 
	 * @param hubLink
	 *            Distributed Hub URI
	 * @param endpoints
	 *            list of endpoints to add
	 * @return endpoints registered on this machine
	 */
	@SuppressWarnings("unchecked")
	@POST
	@Path("link")
	@Produces(MediaType.APPLICATION_JSON)
	public final String linkToHub(
			@FormParam(JERSEY_POST_LINK_HUBURL) String hubLink,
			@FormParam(JERSEY_POST_LINK_ENDPOINTS) String jsonEndpoints) {
		if (hubLink == null) {
			return null;
		}

		// register new endpoints
		if (jsonEndpoints != null) {
			try {
				for (Entry<String, String> entry : ((Map<String, String>) json
						.fromJSON(jsonEndpoints)).entrySet()) {
					System.out.println("key: " + entry.getKey());
					System.out.println("value: " + entry.getValue());
					hub.getRegistrations().addEndpointByMachineID(
							entry.getValue(),
							hub.getEndpointDescriptionFromJSON(json
									.fromJSON(entry.getKey())));
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}

		}
		//add connection to hub
		distributedHub.addConnectedHub(hubLink);
		Map<String, String> allEndpoinsJSON = new HashMap<String, String>();
		//send registered endpoints in JSON
		for (Entry<EndpointDescription, String> entry : hub.getRegistrations()
				.getAllEndpoints().entrySet()) {
			allEndpoinsJSON.put(json.toJSON(entry.getKey().getProperties()),
					entry.getValue());
		}
		return json.toJSON(allEndpoinsJSON);
	}

	@GET
	@Path("endpoints/{publisher}")
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
	@Path("endpoints/{publisher}")
	public final Response newEndpoint(
			@FormParam(JERSEY_POST_PARAMETER_ENDPOINT) String endp,
			@PathParam("publisher") String publisher) {
		EndpointDescription endpoint;
		try {
			endpoint = hub.getEndpointDescriptionFromJSON(json.fromJSON(endp));
			// if endpoints already registered, don`t notify other hubs
			// (prevents looping)
			if (hub.getRegistrations().addEndpointByMachineID(publisher,
					endpoint)) {
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return Response.ok().build();
	}

	@DELETE
	@Path("endpoints/{publisher}/{id}")
	public final Response removeEndpoint(@PathParam("id") long endpointId,
			@PathParam("publisher") String publisher) {
		// if endpoints already registered, don`t notify other hubs (prevents
		// looping)
		if (hub.getRegistrations().removeEndpoint(publisher, endpointId)) {
			distributedHub.removeEndpoint(endpointId, publisher);
		}
		return Response.ok().build();
	}

}
