package org.ow2.chameleon.rose.pubsubhubbub.subscriber.internal;

import static org.ow2.chameleon.rose.constants.RoseRSSConstants.HTTP_POST_HEADER_TYPE;
import static org.ow2.chameleon.rose.constants.RoseRSSConstants.HTTP_POST_UPDATE_CONTENT;
import static org.ow2.chameleon.rose.constants.RoseRSSConstants.HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION;
import static org.ow2.chameleon.rose.pubsubhubbub.subscriber.Subscriber.COMPONENT_NAME;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.http.HttpStatus;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseEndpointDescription;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.constants.RoseRSSConstants;
import org.ow2.chameleon.rose.pubsubhubbub.subscriber.Subscriber;
import org.ow2.chameleon.rose.util.DefaultLogService;

/**
 * Listen to all matched endpoints from hub and register them in Rose.
 * 
 * @author Bartek
 * 
 */
@Component(name = COMPONENT_NAME)
public class RSSEndpointListener extends HttpServlet implements Subscriber {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6485125168680274690L;

	@Requires
	private transient HttpService httpService;

	@Requires
	private transient RoseMachine rose;

	@Requires
	private transient JSONService json;

	@Requires(optional = true, defaultimplementation = DefaultLogService.class)
	private transient LogService logger;

	@Property(name = INSTANCE_PROPERTY_CALLBACK_URL)
	private String callBackUrl;

	@Property(name = INSTANCE_PROPERTY_HUB_URL)
	private String hubUrl;

	@Property(name = INSTANCE_PROPERTY_ENDPOINT_FILTER)
	private String endpointFilter;

	private transient HubSubscriber subscritpion;
	private transient BundleContext context;
	private int responseCode;
	private List<String> endpointRegistrations;

	public RSSEndpointListener(final BundleContext pContext) {
		this.context = pContext;
	}

	@Override
	@Validate
	public final void start() throws Exception {
		endpointRegistrations = new ArrayList<String>();
		httpService.registerServlet(callBackUrl, this, null, null);
		subscritpion = new HubSubscriber(hubUrl, callBackUrl, endpointFilter,
				context, rose);
	}

	@Override
	@Invalidate
	public final void stop() {
		httpService.unregister(callBackUrl);
		try {
			if (subscritpion != null) {
				subscritpion.unsubscribe();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (String endpoint : endpointRegistrations) {
			rose.removeRemote(endpoint);
		}
	}

	@Override
	protected final void doPost(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {

		if ((!(req.getHeader("Content-Type").equals(HTTP_POST_HEADER_TYPE)))
				|| (req.getParameter(HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION) == null)
				|| (req.getParameter(HTTP_POST_UPDATE_CONTENT) == null)) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			return;
		} else {

			try {
				@SuppressWarnings("unchecked")
				EndpointDescription endp = RoseEndpointDescription
						.getEndpointDescription(json.fromJSON(req
								.getParameter(HTTP_POST_UPDATE_CONTENT)));
				if (req.getParameter(HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION)
						.equals(RoseRSSConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_ADDED)) {
					rose.putRemote(endp.toString(), endp);
					endpointRegistrations.add(endp.toString());
					logger.log(LogService.LOG_INFO,
							"Remote endpoint " + endp.getId() + " added");
				} else if (req
						.getParameter(HTTP_POST_UPDATE_SUBSTRIPCTION_OPTION)
						.equals(RoseRSSConstants.HUB_SUBSCRIPTION_UPDATE_ENDPOINT_REMOVED)) {
					rose.removeRemote(endp.toString());
					endpointRegistrations.remove(endp.toString());
					logger.log(LogService.LOG_INFO,
							"Remote endpoint " + endp.getId() + " removed");
				}
				this.responseCode = HttpStatus.SC_OK;
			} catch (ParseException e) {
				this.responseCode = HttpStatus.SC_BAD_REQUEST;
				e.printStackTrace();
			}
		}

		resp.setStatus(responseCode);

	}
}
