package org.ow2.chameleon.rose.pubsubhubbub.client;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;

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
import org.osgi.framework.Constants;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.ow2.chameleon.json.JSONService;


@Component(name="Rose_Pubsubhubbub.subscriber")
public class RSSEndpointListener extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6485125168680274690L;

	@Requires
	HttpService httpService;
	
	@Requires
	JSONService json;
	
	@Property(name="callback.url")
	private String callBackUrl;
	
	@Property(name="hub.url")
	private String hubUrl;
	
	private String endpointfilter = "(endpoint.id=*)";
	private Subscriber subscritpion;
	private BundleContext context;
	private int responseCode;
	
	public RSSEndpointListener(BundleContext context){
		this.context=context;
	}
	
	@Validate
	void start(){
		try {
			httpService.registerServlet(callBackUrl, this, null, null);
			subscritpion = new Subscriber(hubUrl,callBackUrl,endpointfilter,context);
		} catch (ServletException e) {
			e.printStackTrace();
		} catch (NamespaceException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Invalidate
	void stop(){
		httpService.unregister(callBackUrl);
		try {
			subscritpion.unsubscribe();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		if ((!(req.getHeader("Content-Type")
				.equals("application/x-www-form-urlencoded")))
				|| (req.getParameter("subscription") == null)||(req.getParameter("content")==null)) {
			resp.setStatus(HttpStatus.SC_BAD_REQUEST);
			return;
		}
		else{

			try {
				@SuppressWarnings("unchecked")
				EndpointDescription endp = getEndpointDescriptionFromJSON(json.fromJSON(req.getParameter("content")));
				if(req.getParameter("subscription").equals("endpoint.add")){
					System.out.println("add endpoint");
					System.out.println(endp);
				}
				else if(req.getParameter("subscription").equals("endpoint.remove")){
					System.out.println("add remove");
					System.out.println(endp);
				}
				this.responseCode=HttpStatus.SC_OK;
			} catch (ParseException e) {
				this.responseCode=HttpStatus.SC_BAD_REQUEST;
				e.printStackTrace();
			}
		}

		resp.setStatus(responseCode);
		
	}
	
	@SuppressWarnings("unchecked")
	private EndpointDescription getEndpointDescriptionFromJSON(
			Map<String, Object> map) {

		if (map.get(Constants.OBJECTCLASS) instanceof ArrayList<?>) {
			map.put(Constants.OBJECTCLASS, ((ArrayList<String>) map
					.get(Constants.OBJECTCLASS)).toArray(new String[0]));
		}

		if (map.get(RemoteConstants.ENDPOINT_SERVICE_ID) instanceof Integer) {
			Integer id = (Integer) map
					.get((RemoteConstants.ENDPOINT_SERVICE_ID));
			map.put(RemoteConstants.ENDPOINT_SERVICE_ID, id.longValue());
		}
		return new EndpointDescription(map);
	}
	
	
}
