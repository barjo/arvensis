package org.ow2.chameleon.rose.jsonrpc;

import static java.util.Arrays.asList;
import static org.osgi.service.log.LogService.LOG_ERROR;
import static org.ow2.chameleon.rose.introspect.EndpointCreatorIntrospection.ENDPOINT_CONFIG_PREFIX;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.jabsorb.JSONRPCBridge;
import org.jabsorb.JSONRPCServlet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.AbstractEndpointCreator;
import org.ow2.chameleon.rose.ExporterService;

@Component(name="rose.jsonrpc.endpointcreator.factory")
@Instantiate(name="rose.jsonrpc.endpointcreator-jabsorb")
@Provides(specifications=ExporterService.class)
@StaticServiceProperty(mandatory=true,name=ENDPOINT_CONFIG_PREFIX,type="String[]",value="{json-rpc,jsonrpc,org.jabsorb}")
public class EndpointCreator extends AbstractEndpointCreator implements ExporterService {
	
	/**
	 * Property containing the URL of the JSONRPC orb.
	 */
	private final static String PROP_JABSORB_URL = "org.jabsorb.url";
	
	/**
	 * Configuration supported by this component
	 */
	private final static List<String> configs = asList("json-rpc","jsonrpc","org.jabsorb");
	
    /**
     * Name of the Property needed by JSONRPCServlet, the gzip threshold.
     */
    private static final String PROP_GZIP_THRESHOLD = "gzip.threshold";

    /**
     * Values of the Property needed by JSONRPCServlet, the gzip threshold.
     */
    @ServiceProperty(name=PROP_GZIP_THRESHOLD,mandatory=true,value="200")
    private String gzip_threshold;
    
    /**
     * The Servlet name of the JSON-RPC bridge.
     */
    @ServiceProperty(name="jsonrpc.servlet.name",mandatory=true,value="/JSONRPC")
    private String servletname;
    
    /**
     * This class implements a bridge that unmarshalls JSON objects in JSON-RPC
     * request format, invokes a method on the exported object, and then
     * marshalls the resulting Java objects to JSON objects in JSON-RPC result
     * format.
     */
    private JSONRPCBridge jsonbridge;
    
    /**
     * Set which contains the names of the endpoint created by this factory.
     */
    private Set<String> endpointIds = new HashSet<String>();
    
	@Requires(optional=true)
	private LogService logger;
	
	@Requires(optional=true)
	private EventAdmin eventadmin;
	
	@Requires(optional=false)
	private HttpService httpservice;
	
	public EndpointCreator(BundleContext pContext) {
		super(pContext);
	}
	
	/*--------------------------*
	 *  Component LifeCycle     *
	 *--------------------------*/
	
	@Override
	protected void start() {
		super.start();

        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(PROP_GZIP_THRESHOLD, gzip_threshold);

        try {
            // Registered the JSONRPCServlet
            httpservice.registerServlet(servletname, new JSONRPCServlet(), properties, null);
        } catch (NamespaceException e) {
            logger.log(LOG_ERROR, e.getMessage(), e);
        } catch (Exception e) {
            logger.log(LOG_ERROR, e.getMessage(), e);
        }

        // Set the bridge to a global bridge. (HttpSession are not managed here)
        // TODO support the HttpSession
        jsonbridge = JSONRPCBridge.getGlobalBridge();
	}
	
	@Override
	protected void stop() {
		super.stop();
		
        try { //Unregister the jsonrpc server.
            if (httpservice != null) {
                httpservice.unregister(servletname);
            }
        } catch (RuntimeException re) {
            logger.log(LogService.LOG_ERROR, re.getMessage(), re);
        }
	}

	/*----------------------------*
	 *  EndpointCreator methods   *
	 *----------------------------*/
	
	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.AbstractEndpointCreator#createEndpoint(org.osgi.framework.ServiceReference, java.util.Map)
	 */
	protected EndpointDescription createEndpoint(ServiceReference sref,
			Map<String, Object> extraProperties) {

		//Set the url property
		//extraProperties.put(PROP_JABSORB_URL, ""); //FIXME
		
		//create the endpoint description
		EndpointDescription desc = new EndpointDescription(sref, extraProperties);
		
        String id = desc.getId();
        
        Object service = getBundleContext().getService(sref);
        
        //Check if the name is valid
        if (endpointIds.contains(id)){
            throw new IllegalArgumentException("An endpoint of id: "+id+" has already been created. Please chose a different id.");
        }
        
        if (desc.getInterfaces().size() > 1) {
            // Export all !
            jsonbridge.registerObject(id, service);
        }

        //only the specified interface
        else {
            String itf = desc.getInterfaces().get(0);

            try {
                jsonbridge.registerObject(id, service, sref.getBundle().loadClass(itf));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cannot load the service interface " + itf + " from the service classloader.", e);
            }
        }
        
        endpointIds.add(id); //add the id to the list
		return desc;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.AbstractEndpointCreator#destroyEndpoint(org.osgi.service.remoteserviceadmin.EndpointDescription)
	 */
	protected void destroyEndpoint(EndpointDescription endesc) {
        //Destroy the endpoint
		if (endpointIds.remove(endesc.getId())){
			jsonbridge.unregisterObject(endesc.getId());
		}
        
	}
	
	
	/*-----------------*
	 * Service Getter  *
	 *-----------------*/
	
	
	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.AbstractEndpointCreator#getLogService()
	 */
	protected LogService getLogService() {
		return logger;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.AbstractEndpointCreator#getEventAdmin()
	 */
	protected EventAdmin getEventAdmin() {
		return eventadmin;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.introspect.EndpointCreatorIntrospection#getConfigPrefix()
	 */
	public List<String> getConfigPrefix() {
		return configs;
	}
}

