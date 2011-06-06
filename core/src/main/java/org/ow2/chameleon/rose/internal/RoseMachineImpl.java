package org.ow2.chameleon.rose.internal;

import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.ow2.chameleon.rose.util.RoseTools.getAllExporter;
import static org.ow2.chameleon.rose.util.RoseTools.getAllImporter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
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
import org.ow2.chameleon.rose.registry.ExportRegistryListening;
import org.ow2.chameleon.rose.registry.ExportRegistryProvisioning;
import org.ow2.chameleon.rose.registry.ImportRegistryListening;
import org.ow2.chameleon.rose.registry.ImportRegistryProvisioning;
import org.ow2.chameleon.rose.util.DefaultLogService;
import org.ow2.chameleon.rose.util.RoseTools;


@Component(name="RoSe.machine",immediate=true)
@Instantiate
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
	
	private final Map<Object,ServiceRegistration> registrations;
	
	/**
	 * The machine properties.
	 */
	private final Hashtable<String, Object> properties;
	
	private final BundleContext context;
	
	@Requires(optional=true,defaultimplementation=DefaultLogService.class)
	private LogService logger;
	
	public RoseMachineImpl(BundleContext pContext) {
		properties = new Hashtable<String, Object>(4);
		registrations = new HashMap<Object, ServiceRegistration>(4);
		context = pContext;
		
		//Initialize the machine properties.
		initProperties(context);
		
		//Create the import registry
		importReg = new ImportRegistryImpl();
		
		//Create the export registry
		exportReg = new ExportRegistryImpl(context);
		
		//Create the EndpointListener Tracker
		tracklistener = new EndpointListenerTracker(context, importReg,exportReg);
	}

	@SuppressWarnings("unused")
	@Validate
	private void start(){
		//Register the import service and export service registries.
		registrations.put(exportReg, context.registerService(new String[]{ExportRegistryListening.class.getName(),ExportRegistryProvisioning.class.getName()},exportReg,properties));
		registrations.put(importReg, context.registerService(new String[]{ImportRegistryListening.class.getName(),ImportRegistryProvisioning.class.getName()},importReg,properties));
		
		//Register the RoseMachine Service
		registrations.put(this, context.registerService(RoseMachine.class.getName(), this, properties));
	
		//Open the EndpointListener tracker
		tracklistener.open();
		
		log(LogService.LOG_INFO,"The RoseMachine "+properties.get(ROSE_MACHINE_ID)+" has successfully started");
	}
	
	@SuppressWarnings("unused")
	@Invalidate
	private void stop(){
		//Unregister the services
		for (ServiceRegistration reg : registrations.values()) {
			reg.unregister();
		}
		
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
			filterb.append(ImporterService.ENDPOINT_CONFIG_PREFIX);
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
	private static final String DEFAULT_IP = "127.0.0.1";

	/**
	 * Default machine Host.
	 */
	private static final String DEFAULT_HOST = "localhost";
	
	
	/**
	 * Initialize the Machine properties thanks to the framework properties.
	 */
	private final void initProperties(BundleContext context){
		final String machineId;
		final String machineHost;
		final String machineIP;
		
		// Initialize machineID
		if (context.getProperty(ROSE_MACHINE_ID) != null) {
			machineId = context.getProperty(ROSE_MACHINE_ID);
		} else if (context.getProperty(ENDPOINT_FRAMEWORK_UUID) != null) {
			machineId = context.getProperty(ENDPOINT_FRAMEWORK_UUID);
		} else {
			machineId = UUID.randomUUID().toString();
		}
		// Initialize machineHost
		if (context.getProperty(ROSE_MACHINE_HOST) != null) {
			machineHost = context.getProperty(ROSE_MACHINE_HOST);
		} else {
			machineHost = DEFAULT_HOST;
		}

		// Initialize machineIP
		if (context.getProperty(ROSE_MACHINE_IP) != null) {
			machineIP = context.getProperty(ROSE_MACHINE_IP);
		} else {
			machineIP = DEFAULT_IP;
		}
		
		properties.put(ROSE_MACHINE_ID, machineId);
		properties.put(ROSE_MACHINE_IP, machineIP);
		properties.put(ROSE_MACHINE_HOST, machineHost);
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
	 * @return This rose machine ip.
	 */
	public final String getIP() {
		return (String) properties.get(ROSE_MACHINE_IP);
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
	
	public void log(int level, String message){
		logger.log(level, message);
	}
	
	public void log(int level, String message,Throwable exception){
		logger.log(level, message,exception);
	}

}
