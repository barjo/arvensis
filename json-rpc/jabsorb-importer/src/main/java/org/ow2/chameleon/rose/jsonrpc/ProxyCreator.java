package org.ow2.chameleon.rose.jsonrpc;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static org.ow2.chameleon.rose.util.RoseTools.registerProxy;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabsorb.client.Client;
import org.jabsorb.client.HTTPSession;
import org.jabsorb.client.Session;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.AbstractImporterComponent;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.util.RoseTools;

/**
 * Provides an {@link ImporterService} allowing to access a
 * remote endpoint through jsonrpc thanks to the jabsorb implementation. 
 * 
 * TODO Improve the client management, only one client should be created for a given uri.
 */
public class ProxyCreator extends AbstractImporterComponent{
	/**
	 * Property containing the URL of the JSONRPC orb.
	 */
	private final static String PROP_JABSORB_URL = "org.jabsorb.url";
	
	private static final String[] CONFIGS = {"jsonrpc", "org.jabsorb", "json-rpc"};
	
	private final BundleContext context;

    /**
     * Map which contains the proxies and theirs Client.
     */
    private HashMap<String, Client> proxies;

    public ProxyCreator(BundleContext pContext) {
        proxies = new HashMap<String, Client>();
        context=pContext;
    }
    
    public ServiceRegistration createProxy(EndpointDescription description,Map<String, Object> properties){
    	final Object proxy;
        final Client client;
        
    	// Get the endpoint properties
    	String uri = valueOf(description.getProperties().get(PROP_JABSORB_URL));
        if (uri == null){
         	uri = valueOf(properties.get(PROP_JABSORB_URL));
        }
        
        //Try to load the class
        final List<Class<?>> klass = RoseTools.loadClass(context, description);
        
        if (klass == null){
			throw new IllegalStateException(
					"Cannot create a proxy for the description: "
							+ description
							+ " unable to find a bundle which export the service class.");
        }
        
        try {
            Session session = new HTTPSession(new URI(uri));
            client = new Client(session);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("The property" + PROP_JABSORB_URL + "must be set and a valid String form of the endpoint URL", e);
        }
        

        // Create the proxy thanks to jabsorb
        // FIXME implements only the first interface
        proxy = client.openProxy(description.getId(), klass.get(0));

        // Add the proxy to the proxy list
        proxies.put(description.getId(), client);

        return registerProxy(context, proxy,description,properties);
    } 

    public void destroyProxy(EndpointDescription description, ServiceRegistration registration){
    	if (proxies.containsKey(description.getId())) {
            Client client = proxies.remove(description.getId());
            // Close the proxy
            client.closeProxy(description.getId());
        } else {
            throw new IllegalArgumentException("The given object has not been created through this factory");
        }
    }
    
	
	public List<String> getConfigPrefix() {
		return asList(CONFIGS);
	}


    /*------------------------------------------*
     *  Component LifeCycle method              *
     *------------------------------------------*/
	
	protected void start(){
		super.start();
	}

    /**
     * CallBack onInvalidate, called by iPOJO. Destroy all the created proxy.
     */
    @SuppressWarnings("unused")
	protected void stop() {
        super.stop();
    }

	@Override
	protected LogService getLogService() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected RoseMachine getRoseMachine() {
		// TODO Auto-generated method stub
		return null;
	}
    

}
