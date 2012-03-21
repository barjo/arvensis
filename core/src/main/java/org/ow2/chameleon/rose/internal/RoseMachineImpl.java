package org.ow2.chameleon.rose.internal;

import static java.lang.Integer.MAX_VALUE;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.ow2.chameleon.rose.RoSeConstants.ENDPOINT_CONFIG;
import static org.ow2.chameleon.rose.util.RoseTools.getAllExporter;
import static org.ow2.chameleon.rose.util.RoseTools.getAllImporter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.util.DefaultLogService;
import org.ow2.chameleon.rose.util.RoseTools;


@Component(name="RoSe_machine", immediate=true)
public class RoseMachineImpl implements RoseMachine,RemoteServiceAdmin{

	/**
	 * Remote EndpointDescription dynamic registry.
	 */
	protected final ImportRegistryImpl importReg;
	
	/**
	 * Local EndpointDescriptoin dynamic registry
	 */
	protected final ExportRegistryImpl exportReg;

	/**
	 * Track the {@link EndpointListener} and notified them about the
	 * {@link EndpointDescription} availabilities.
	 */
	private final EndpointListenerTracker tracklistener;
	
	/**
	 * ServiceRegistration of the services registered by this component. 
	 * {@link RoseMachine} and {@link RemoteServiceAdmin}
	 */
	private final Set<ServiceRegistration> registrations;
	
	/**
	 * The machine properties.
	 */
	private final Hashtable<String, Object> properties;
	
	private final BundleContext context;
	
	@Requires(optional=true,defaultimplementation=DefaultLogService.class)
	private LogService logger;
	
	public RoseMachineImpl(BundleContext pContext) {
		properties = new Hashtable<String, Object>(5);
		registrations = new HashSet<ServiceRegistration>(2);
		context = pContext;
		
		//Create the import registry
		importReg = new ImportRegistryImpl();
		
		//Create the export registry
		exportReg = new ExportRegistryImpl(context);
		
		//Create the EndpointListener Tracker
		tracklistener = new EndpointListenerTracker(this);
	}

	@Validate
	private void start(){
		//Initialize the machine properties.
		initProperties(context);
		
		//Register the import service and export service registries.
		//registrations.put(exportReg, context.registerService(new String[]{ExportRegistryListening.class.getName(),ExportRegistryProvisioning.class.getName()},exportReg,properties));
		//registrations.put(importReg, context.registerService(new String[]{ImportRegistryListening.class.getName(),ImportRegistryProvisioning.class.getName()},importReg,properties));
		
		//Register the RoseMachine Service
		registrations.add(context.registerService(RoseMachine.class.getName(), this, properties));
		
		//Register the RemoteServiceAdmin Service
		registrations.add(context.registerService(RemoteServiceAdmin.class.getName(), this, properties));
		
	
		//Open the EndpointListener tracker
		tracklistener.open();
		
		log(LogService.LOG_INFO,"The RoseMachine "+properties.get(ROSE_MACHINE_ID)+" has successfully started");
	}
	
	@Invalidate
	private void stop(){
		//Unregister the services
		for (ServiceRegistration reg : registrations) {
			reg.unregister();
		}
		
		//Clear the registrations set
		registrations.clear(); 
		
		//Close the EndpointListener tracker
		tracklistener.close();
		
		//Stop both rose local registry
		importReg.stop();
		exportReg.stop();
		
		log(LogService.LOG_INFO,"The RoseMachine "+properties.get(ROSE_MACHINE_ID)+" has been stoped");
	}
	

	/*-------------------------------*
	 *  Registry methods             *
	 *-------------------------------*/
	
	
	public void putRemote(Object key, EndpointDescription description) {
		importReg.put(key, description);
	}

	public EndpointDescription removeRemote(Object key) {
		return importReg.remove(key);
	}

	public boolean containsRemote(EndpointDescription desc) {
		return importReg.contains(desc);
	}

	
	public void putLocal(Object key, ExportReference xref) {
		exportReg.put(key, xref);
	}

	public ExportReference removeLocal(Object key) {
		return exportReg.remove(key);
	}

	public boolean containsLocal(ExportReference xref) {
		return exportReg.contains(xref);
	}
	
	public void addEndpointListener(EndpointListener listener,
			EndpointListerInterrest interrest, String filter)
			throws InvalidSyntaxException {
		switch (interrest) {
		case ALL:
			exportReg.addEndpointListener(listener, filter);
			importReg.addEndpointListener(listener, filter);
			break;
		case LOCAL:
			exportReg.addEndpointListener(listener, filter);
			break;
		case REMOTE:
			importReg.addEndpointListener(listener, filter);
		}
	}

	public void removeEndpointListener(EndpointListener listener,
			EndpointListerInterrest interrest) {
		switch (interrest) {
		case ALL:
			exportReg.removeEndpointListener(listener);
			importReg.removeEndpointListener(listener);
			break;
		case LOCAL:
			exportReg.removeEndpointListener(listener);
			break;
		case REMOTE:
			importReg.removeEndpointListener(listener);
		}
	}
	

	/*------------------------------*
	 *  RemoteServiceAdmin methods  *
	 *------------------------------*/
	
	public Collection<ExportRegistration> exportService(ServiceReference reference, Map<String, Object> properties) {
		Collection<ExporterService> exporters = RoseTools.getAllExporter(context);
		Collection<ExportRegistration> registrations = new HashSet<ExportRegistration>(); 
		
		for (ExporterService exporter : exporters) {
			ExportRegistration reg = exporter.exportService(reference, properties);
			if (reg!=null){
				registrations.add(reg);
			}
		}

		return registrations;
	}

	public ImportRegistration importService(EndpointDescription endpoint) {
		Iterator<ImporterService> iterator;
		
		//Construct a Filter which track only the Importer which are compliant with the EndpointDescription.
		StringBuilder filterb = new StringBuilder("(&");
		
		for (String conf : endpoint.getConfigurationTypes()) {
			filterb.append("(");
			filterb.append(ENDPOINT_CONFIG);
			filterb.append("=");
			filterb.append(conf);
			filterb.append(")");
		}
		filterb.append(")");
		
		try {
			iterator = getAllImporter(context, filterb.toString()).iterator();
		} catch (InvalidSyntaxException e) {
			assert false; //What Would Dr. Gordon Freeman Do ?
			return null;
		}
		
		ImportRegistration registration = null;

		//First successful import is the winner :P
		while (iterator.hasNext() && registration==null) {
			registration = iterator.next().importService(endpoint,null);
		}
		
		return registration;
	}

	public Collection<ExportReference> getExportedServices() {
		Collection<ExporterService> exporters = getAllExporter(context);
		Collection<ExportReference> refs = new HashSet<ExportReference>();
		
		for (ExporterService exporter : exporters) {
			refs.addAll(exporter.getAllExportReference());
		}
		
		return refs;
	}

	public Collection<ImportReference> getImportedEndpoints() {
		Collection<ImportReference> refs = new HashSet<ImportReference>();
		List<ImporterService> importers = getAllImporter(context);
		
		for (ImporterService importer : importers) {
			refs.addAll(importer.getAllImportReference());
		}
		
		return refs;
	}
	
	/*------------------------------*
	 * The RoSe Machine Properties  *
	 *------------------------------*/
	
	/**
	 * Default machine IP.
	 */
	//private static final String DEFAULT_IP = "127.0.0.1";

	/**
	 * Default machine Host.
	 */
	private static final String DEFAULT_HOST = "localhost";
	
	@Property(name=ROSE_MACHINE_HOST)
	private String myhost;
	
	@Property(name=ROSE_MACHINE_ID)
	private String myid;
	
	
	/**
	 * Initialize the Machine properties thanks to the framework properties.
	 */
	private final void initProperties(BundleContext context){
		
		// Initialize machineID
		if (myid==null){
			if (context.getProperty(ROSE_MACHINE_ID) != null) {
				myid = context.getProperty(ROSE_MACHINE_ID);
			} else if (context.getProperty(ENDPOINT_FRAMEWORK_UUID) != null) {
				myid = context.getProperty(ENDPOINT_FRAMEWORK_UUID);
			} else {
				myid = UUID.randomUUID().toString();
			}
		}
		
		
		// Initialize machineHost
		if (myhost==null){
			if (context.getProperty(ROSE_MACHINE_HOST) != null) {
				myhost = context.getProperty(ROSE_MACHINE_HOST);
			} else {
				myhost = DEFAULT_HOST;
			}
		}

		properties.put(ROSE_MACHINE_ID, myid);
		properties.put(SERVICE_PID, myhost+"-"+myid);
		properties.put(SERVICE_VENDOR, "org.ow2.chameleon");
		properties.put(SERVICE_RANKING, MAX_VALUE);
		properties.put(ROSE_MACHINE_HOST, myhost);
	}
	
	/**
	 * @return This rose machine id.
	 */
	public final String getId() {
		return (String) properties.get(ROSE_MACHINE_ID);
	}

	/**
	 * @return This rose machine host.
	 */
	public final String getHost() {
		return (String) properties.get(ROSE_MACHINE_HOST);
	}

	/**
	 * @return This RoSe machine properties.
	 */
	public final Map<String, Object>  getProperties(){
		return Collections.unmodifiableMap(properties);
	}
	
	/*---------------------------------------*
	 *  RoseMachine Logger                   *
	 *---------------------------------------*/
	
	protected void log(int level, String message){
		logger.log(level, message);
	}
	
	protected void log(int level, String message,Throwable exception){
		logger.log(level, message,exception);
	}
	
	/*---------------------------------------*
	 * RoseMachine BundleContext             *
	 *---------------------------------------*/
	
	protected BundleContext getContext(){
		return context;
	}
	
}
