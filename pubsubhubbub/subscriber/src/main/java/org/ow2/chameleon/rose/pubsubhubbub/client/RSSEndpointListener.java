package org.ow2.chameleon.rose.pubsubhubbub.client;

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
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseEndpointDescription;
import org.ow2.chameleon.rose.RoseMachine;


/**Listen to all matched endpoints from hub and register them in Rose.
 * @author Bartek
 *
 */
@Component(name="Rose_Pubsubhubbub.subscriber")
public class RSSEndpointListener extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6485125168680274690L;

	@Requires
	private HttpService httpService;
	
	@Requires
	private RoseMachine machine;
	
	@Requires
	private JSONService json;
	
	@Requires
	private LogService logger;
	
	@Property(name="callback.url")
	private String callBackUrl;
	
	@Property(name="hub.url")
	private String hubUrl;
	
	private String endpointfilter;
	private Subscriber subscritpion;
	private BundleContext context;
	private int responseCode;
	private List<String> endpointRegistrations;
	
	public RSSEndpointListener(BundleContext context){
		this.context=context;
	}
	
	@Validate
	void start(){
		try {
			if((endpointfilter=machine.getImportEndpointFilter())!=null){
			endpointRegistrations = new ArrayList<String>();
			httpService.registerServlet(callBackUrl, this, null, null);
			subscritpion = new Subscriber(hubUrl,callBackUrl,endpointfilter,context);
			}
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
		for (String endpoint : endpointRegistrations) {
			machine.removeRemote(endpoint);
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
				EndpointDescription endp =  RoseEndpointDescription
				.getEndpointDescription(json.fromJSON(req.getParameter("content")));
				if(req.getParameter("subscription").equals("endpoint.add")){
					machine.putRemote(endp.toString(), endp);
					endpointRegistrations.add(endp.toString());
					logger.log(LogService.LOG_INFO, "Remote endpoint "+endp.getId()+" added");
				}
				else if(req.getParameter("subscription").equals("endpoint.remove")){
					machine.removeRemote(endp.toString());
					endpointRegistrations.remove(endp.toString());
					logger.log(LogService.LOG_INFO, "Remote endpoint "+endp.getId()+" removed");
				}
				this.responseCode=HttpStatus.SC_OK;
			} catch (ParseException e) {
				this.responseCode=HttpStatus.SC_BAD_REQUEST;
				e.printStackTrace();
			}
		}

		resp.setStatus(responseCode);
		
	}	
}
