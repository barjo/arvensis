package org.ow2.chameleon.rose.internal;

import static org.osgi.service.log.LogService.LOG_ERROR;
import static org.osgi.service.remoteserviceadmin.EndpointListener.ENDPOINT_LISTENER_SCOPE;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
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
import org.ow2.chameleon.rose.registry.ExportRegistry;
import org.ow2.chameleon.rose.registry.ExportRegistryListening;
import org.ow2.chameleon.rose.registry.ExportRegistryProvisioning;
import org.ow2.chameleon.rose.registry.ImportRegistry;
import org.ow2.chameleon.rose.registry.ImportRegistryListening;
import org.ow2.chameleon.rose.registry.ImportRegistryProvisioning;
import org.ow2.chameleon.rose.util.DefaultLogService;

@Component(name="RoSe.machine",immediate=true)
@Instantiate
public class RoseMachineImpl implements RoseMachine,RemoteServiceAdmin{

	/**
	 * Remote EndpointDescription dynamic registry.
	 */
	private final ImportRegistryImpl importReg;
	
	/**
	 * Local EndpointDescriptoin dynamic registry
	 */
	private final ExportRegistryImpl exportReg;
	
	private final Map<Object,ServiceRegistration> registrations;
	
	/**
	 * The machine properties.
	 */
	private final Hashtable<String, Object> properties;
	
	private Map<String, ExporterService> exporters;
	
	private Map<String, ImporterService> importers;
	
	
	private final BundleContext context;
	
	@Requires(optional=true,defaultimplementation=DefaultLogService.class)
	private LogService logger;
	
	public RoseMachineImpl(BundleContext pContext) {
		exporters = new HashMap<String, ExporterService>();
		properties = new Hashtable<String, Object>(4);
		registrations = new HashMap<Object, ServiceRegistration>(4);
		context = pContext;
		
		//Initialize the machine properties.
		initProperties(context);
		
		//Create the import registry
		importReg = new ImportRegistryImpl();
		
		//Create the export registry
		exportReg = new ExportRegistryImpl(context);
		
	}

	@SuppressWarnings("unused")
	@Validate
	private void start(){
		//Register the import service and export service registries.
		registrations.put(exportReg, context.registerService(new String[]{ExportRegistryListening.class.getName(),ExportRegistryProvisioning.class.getName()},exportReg,properties));
		registrations.put(importReg, context.registerService(new String[]{ImportRegistryListening.class.getName(),ImportRegistryProvisioning.class.getName()},importReg,properties));
		
		//Register the RoseMachine Service
		registrations.put(this, context.registerService(RoseMachine.class.getName(), this, properties));
	}
	
	@SuppressWarnings("unused")
	@Invalidate
	private void stop(){
		//Unregister the services
		for (ServiceRegistration reg : registrations.values()) {
			reg.unregister();
		}
		
		//Stop both rose local registry
		importReg.stop();
		exportReg.stop();
	}

	/*-------------------------------*
	 *                               *
	 *-------------------------------*/
	
	public final ImportRegistry importRegistry(){
		return importReg;
	}
	
	public final ExportRegistry exportRegistry(){
		return exportReg;
	}
	
	@SuppressWarnings("unused")
	@Bind(optional=true,aggregate=true)
	private void bindEndpointListener(EndpointListener listener, ServiceReference ref){
		Object sfilter = ref.getProperty(ENDPOINT_LISTENER_SCOPE);
		try {
			if (sfilter == null) {
				exportReg.addEndpointListener(listener);
			} else {
				exportReg.addEndpointListener(listener, String.valueOf(sfilter));
			}
		} catch (Exception e) {
			logger.log(
					LOG_ERROR,
					"Cannot add the EndpointListener to the RoseMachine registry. an exception occured",
					e);
		}
	}
	
	@SuppressWarnings("unused")
	@Unbind
	private void unbindEndpointListener(EndpointListener listener){
		exportReg.removeEndpointListener(listener);
	}
	
	/*-----------------------------*
	 *  Server (Service Exporter) methods
	 *-----------------------------*/
	
	public Collection<ExportRegistration> exportService(ServiceReference reference, Map<String, Object> properties) {
		Collection<ExportRegistration> registrations = new HashSet<ExportRegistration>(); 
		
		for (ExporterService exporter : exporters.values()) {
			ExportRegistration reg = exporter.exportService(reference, properties);
			if (reg!=null){
				registrations.add(reg);
			}
		}

		return registrations;
	}

	public ImportRegistration importService(EndpointDescription endpoint) {
		Iterator<String> configs = endpoint.getConfigurationTypes().iterator();
		ImportRegistration registration = null;

		while (configs.hasNext() && registration==null) {
			String config = configs.next();
			registration = importers.get(config).importService(endpoint);
		}
		
		return registration;
	}

	public Collection<ExportReference> getExportedServices() {
		Collection<ExportReference> refs = new HashSet<ExportReference>();
		
		for (ExporterService exporter : exporters.values()) {
			refs.addAll(exporter.getAllExportReference());
		}
		
		return refs;
	}

	public Collection<ImportReference> getImportedEndpoints() {
		Collection<ImportReference> refs = new HashSet<ImportReference>();

		for (ImporterService importer : importers.values()) {
			refs.addAll(importer.getAllImportReference());
		}
		
		return refs;
	}
	
	/*------------------------------*
	 * The RoSe Machine Properties  *
	 *------------------------------*/
	
	/**
	 * System property identifying the ID for this rose machine.
	 */
	public final static String ROSE_MACHINE_ID = "rose.machine.id";

	/**
	 * System property identifying the host name for this rose machine.
	 */
	public final static String ROSE_MACHINE_HOST = "rose.machine.host";

	/**
	 * System property identifying the IP address for this rose machine.
	 */
	public final static String ROSE_MACHINE_IP = "rose.machine.ip";

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

}
