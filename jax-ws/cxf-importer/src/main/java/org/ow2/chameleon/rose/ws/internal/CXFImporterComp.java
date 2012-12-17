package org.ow2.chameleon.rose.ws.internal;

import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.AbstractImporterComponent;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.RoseMachine;

import javax.jws.WebService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.ow2.chameleon.rose.RoSeConstants.ENDPOINT_CONFIG;
import static org.ow2.chameleon.rose.RoSeConstants.ENDPOINT_URL;
import static org.ow2.chameleon.rose.util.RoseTools.loadClass;
import static org.ow2.chameleon.rose.util.RoseTools.registerProxy;

/**
 * This component provides a JAX-WS, Apache CXF based implementation of an
 * {@link ExporterService}.
 * 
 * @author Jonathan Bardin <jonathan.bardin@imag.fr>
 */
@Component(name="RoSe_importer.cxf")
@Provides(specifications={ ImporterService.class })
public class CXFImporterComp extends AbstractImporterComponent implements ImporterService {
	
	/**
	 * Configuration supported by this component
	 */
	@ServiceProperty(name = ENDPOINT_CONFIG, mandatory = true, value = "{jax-ws,org.apache.cxf,cxf,ws}")
	private String[] configs = { "jax-ws", "org.apache.cxf", "cxf", "ws" };
	
    @Requires(optional = true)
	private LogService logger; // inject by iPOJO
    
    /**
	 * Require the {@link RoseMachine}.
	 */
	@Requires(optional = false, id = "rose.machine")
	private RoseMachine machine;

	private BundleContext context;
	

	public CXFImporterComp(BundleContext pContext) {
		context=pContext;
	}
	
	/**
	 * Publish the WS register/dispatcher servlet.
	 * Initialize the cxfbus.
	 */
	@Override
	@Validate
	protected void start() {
		super.start();
	}

	@Override
	@Invalidate
	protected void stop() {
		super.stop();
	}

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.ImporterService#getConfigPrefix()
	 */
	public List<String> getConfigPrefix() {
		return Arrays.asList(configs);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.AbstractImporterComponent#getLogService()
	 */
	protected LogService getLogService() {
		return logger;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.AbstractImporterComponent#getRoseMachine()
	 */
	public RoseMachine getRoseMachine() {
		return machine;
	}

	
	/*--------------------------*
	 * ImporterService methods  *
	 *--------------------------*/

	protected ServiceRegistration createProxy(EndpointDescription description,
			Map<String, Object> extraProperties) {
			
			ClientProxyFactoryBean factory;
		
        	//Try to load the class
            final List<Class<?>> klass = loadClass(context, description);
           
            //use annotations if present
            if (klass.get(0).isAnnotationPresent(WebService.class)){
            	factory = new JaxWsProxyFactoryBean();
            } else {
            	factory = new ClientProxyFactoryBean();
            }
            
			factory.getInInterceptors().add(new LoggingInInterceptor());
			factory.getOutInterceptors().add(new LoggingOutInterceptor());
			
			//set the class XXX only one class is supported
			factory.setServiceClass(klass.get(0));
			
			//set the URL
			factory.setAddress((String) description.getProperties().get(ENDPOINT_URL));
			
			//create the proxy and register it
			return registerProxy(context, factory.create(), description, extraProperties);
	}

	/**
	 *FIXME nothing to release or destroy, is the factory garbage collected ?
	 */
	protected void destroyProxy(EndpointDescription description,
			ServiceRegistration registration) {
		registration.unregister();
	}

}
