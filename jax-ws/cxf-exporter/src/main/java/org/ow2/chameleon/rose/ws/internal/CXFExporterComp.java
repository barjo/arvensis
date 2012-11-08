package org.ow2.chameleon.rose.ws.internal;

import static java.lang.Integer.valueOf;
import static org.osgi.service.log.LogService.LOG_ERROR;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.ow2.chameleon.rose.RoSeConstants.ENDPOINT_CONFIG;
import static org.ow2.chameleon.rose.RoSeConstants.ENDPOINT_URL;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.AbstractExporterComponent;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.introspect.ExporterIntrospection;

/**
 * This component provides a JAX-WS, Apache CXF based implementation of an
 * {@link ExporterService}.
 * 
 * @author Jonathan Bardin <jonathan.bardin@imag.fr>
 */
@Component(name="RoSe_exporter.cxf")
@Provides(specifications={ ExporterService.class, ExporterIntrospection.class })
public class CXFExporterComp extends AbstractExporterComponent implements ExporterService,ExporterIntrospection {
	

	private static final String PROP_HTTP_PORT = "org.osgi.service.http.port";

	private static final Integer DEFAULT_HTTP_PORT = 80;

	/**
	 * Configuration supported by this component
	 */
	@ServiceProperty(name = ENDPOINT_CONFIG, mandatory = true, value = "{jax-ws,org.apache.cxf,cxf,ws}")
	private String[] configs = { "jax-ws", "org.apache.cxf", "cxf", "ws" };
	
    @ServiceProperty(mandatory=true,name="cxf.servlet.name",value="/jaxws")
	private String rootName; 

    
    /**
     * set in {@link CXFExporterComp#bindHttpService(HttpService, ServiceReference)}
     */
	private HttpService httpservice;
	
	//http port of the used httpservice
	private Integer httpport;
	
	/**
     * WebServices will be publish under this url.
     */
    private String myurl;

    @Requires(optional = true)
	private LogService logger; // inject by iPOJO
    
    /**
	 * Require the {@link RoseMachine}.
	 */
	@Requires(optional = false, id = "rose.machine")
	private RoseMachine machine;

	private BundleContext context;
	
	private Bus cxfbus; // init in start

	private Map<String, Server> webservices = new HashMap<String, Server>();

	public CXFExporterComp(BundleContext pContext) {
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
		// Disable the fast infoset as it's not compatible (yet) with OSGi
		//XXX from dosgi
		System.setProperty("org.apache.cxf.nofastinfoset", "true");

		//Register the CXF Servlet
		
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		
		//switch to the cxg minimal bundle class loader
		Thread.currentThread().setContextClassLoader(CXFNonSpringServlet.class.getClassLoader());

		try {
			CXFNonSpringServlet servlet = new CXFNonSpringServlet();

			// Register a CXF Servlet dispatcher
			httpservice.registerServlet(rootName, servlet, null, null);

			// get the bus
			cxfbus = servlet.getBus();

		} catch (Exception e) {
			logger.log(LogService.LOG_ERROR, "Cannot register the CXF Servlet for webservice transport.",e);
			throw new RuntimeException(e);
		} finally {
			Thread.currentThread().setContextClassLoader(loader);
		}
		
		//compute the PROP_CXF_URL property
        try {
			myurl = new URI("http://"+machine.getHost()+":"+httpport+rootName+"/").toString(); //compute the url
		} catch (Exception e) {
			logger.log(LOG_ERROR, "Cannot create the URL of the JAX-WS server, this will lead to incomplete EndpointDescription.",e);
		}
			
		//XXX From this line everything is for test purpose only. 
		//TEST, create a web service
		//createAndRegisterWS(new HelloWorldImpl(), HelloWorld.class, "HelloService");
		/*Thread.currentThread().setContextClassLoader(JaxWsProxyFactoryBean.class.getClassLoader());
		
		try {
			JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
			factory.getInInterceptors().add(new LoggingInInterceptor());
			factory.getOutInterceptors().add(new LoggingOutInterceptor());
			factory.setServiceClass(HelloWorld.class);
			factory.setAddress("http://localhost:8080/jaxws/HelloService");
			HelloWorld client = (HelloWorld) factory.create();

			String reply = client.sayHi("HI");
			System.out.println("Server said: " + reply);
		} finally {
			Thread.currentThread().setContextClassLoader(loader);
		}*/
	}

	@Override
	@Invalidate
	protected void stop() {
		super.stop();
		// Unregister servlet dispatcher
		httpservice.unregister(rootName);
	}
	
	/**
	 * Bind the {@link HttpService} and set the {@link EndpointCreator#httpport} value.
	 * @param service the {@link HttpService}
	 * @param ref the {@link HttpService} {@link ServiceReference}.
	 */
	@Bind(aggregate=false,optional=false)
	private void bindHttpService(HttpService service,ServiceReference ref){
		httpservice = service;
		
		if (ref.getProperty(PROP_HTTP_PORT) != null){
			httpport = valueOf((String) ref.getProperty(PROP_HTTP_PORT));
		}
		else if (System.getProperty(PROP_HTTP_PORT) != null) {
			httpport = valueOf((String) System.getProperty(PROP_HTTP_PORT));
		} else {
			httpport = DEFAULT_HTTP_PORT;
			logger.log( LOG_WARNING, "A default value ("+
					    DEFAULT_HTTP_PORT + 
					    ") has been set to the http port, this could lead to a bad " +
						ENDPOINT_URL + " property value.");
		}
	}

	/*--------------------------*
	 * ExporterService methods  *
	 *--------------------------*/
	

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.AbstractExporterComponent#createEndpoint(org.osgi.framework.ServiceReference, java.util.Map)
	 */
	@Override
	protected EndpointDescription createEndpoint(ServiceReference sref,
			Map<String, Object> extraProperties) {
		
		//get the id
		final String id = ((String) extraProperties.get(ENDPOINT_ID)).trim();
		
		//Set the url property
		extraProperties.put(ENDPOINT_URL, myurl+id);
		
		//Create the EndpointDescription
		EndpointDescription desc = new EndpointDescription(sref,extraProperties);
		
	    Object service = context.getService(sref);
	    
	    //Release the reference
	    context.ungetService(sref);
		
	    if (webservices.containsKey(id)){
	    	throw new IllegalArgumentException("The WS of name "+id+" has already been created and registered");
	    }
	    
	    //If there is only one interface, try to use it
	    Class<?> intf = null;
	    
	    if (desc.getInterfaces().size() == 1){
	    	try{
	    		intf = sref.getBundle().loadClass(desc.getInterfaces().get(0));
	    	}catch (ClassNotFoundException e){
	    		logger.log(LOG_WARNING, "An exception occured while trying to export the service of id"+desc.getServiceId()+" with RoSe_exporter.cxf",e);
	    	}
	    }
	    
	    createAndRegisterWS(service, intf, id);
	     
		return desc;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.AbstractExporterComponent#destroyEndpoint(org.osgi.service.remoteserviceadmin.EndpointDescription)
	 */
	@Override
	protected void destroyEndpoint(EndpointDescription endesc) {
		if (webservices.containsKey(endesc.getId())) {
			webservices.remove(endesc.getId()).stop();
		} else {
			throw new IllegalArgumentException("There is no endpoint of name: "+endesc.getId());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.ExporterService#getConfigPrefix()
	 */
	public List<String> getConfigPrefix() {
		return Arrays.asList(configs);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.AbstractExporterComponent#getLogService()
	 */
	@Override
	protected LogService getLogService() {
		return logger;
	}

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.AbstractExporterComponent#getRoseMachine()
	 */
	public RoseMachine getRoseMachine() {
		return machine;
	}
	
	/*------------------------*
	 *  WS Creation methods   *
	 *------------------------*/
	
	/**
	 * Create and publish an instance of a Service as a WEB-Service thanks to CXF. 
	 * The <code>clazz</code> parameter refers to the interface of the Service. The Web-Service will be 
	 * published on <code>/jaxws/\<endpointName\></code>
	 * 
	 * If the creation of the webservice succeed then the WebService is added to the <code>webservices</code> {@link Map}.
	 * 
	 * @param pService, the instance of the Service to be published as a webservice
	 * @param Clazz, optional parameter which refer to the interface of the Service and used in order to generate the wsdl.
	 * @param endpointName, name of the webservice, used as the suffix of the webservice url.
	 */
	private void createAndRegisterWS(Object pService, Class<?> clazz, String endpointName) {
		
		//Use the classloader of the cxf bundle in order to create the ws.
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(JaxWsServerFactoryBean.class.getClassLoader());
		
		ServerFactoryBean svrFactory;
		
		try {

			//Check if the interface contains the jax-ws annotation
			if (clazz != null && clazz.isAnnotationPresent(WebService.class)){
				svrFactory = new JaxWsServerFactoryBean();
			} else {
				svrFactory = new ServerFactoryBean();
			}
			
			//If the interface has been specified use it in order to create the endpoint.
			if (clazz != null){
				svrFactory.setServiceClass(clazz);
			}
			
			svrFactory.setBus(cxfbus); // Use the OSGi Servlet as the dispatcher
			svrFactory.setServiceBean(pService);
			svrFactory.setAddress("/" + endpointName);
			
			Server res = svrFactory.create(); //Publish the webservice.
			webservices.put(endpointName, res);
		} finally {
			//reset the context classloader to the original one.
			Thread.currentThread().setContextClassLoader(loader);
		}
	}

}
