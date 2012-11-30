package org.ow2.chameleon.rose.internal;

import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.*;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.RoseMachine;

import java.util.*;

import static java.lang.Integer.MAX_VALUE;
import static org.osgi.framework.Constants.*;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.ow2.chameleon.rose.RoSeConstants.RoSe_MACHINE_COMPONENT_NAME;


@Component(name=RoSe_MACHINE_COMPONENT_NAME, immediate=true)
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
	
	@Requires(optional=true)
	private LogService logger;
	
	public RoseMachineImpl(BundleContext pContext) {
		properties = new Hashtable<String, Object>(6);
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
		
		log(LogService.LOG_INFO,"The RoseMachine "+properties.get(RoSe_MACHINE_ID)+" has successfully started");
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
		
		log(LogService.LOG_INFO,"The RoseMachine "+properties.get(RoSe_MACHINE_ID)+" has been stoped");
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

    @Deprecated
	public Collection<ExportRegistration> exportService(ServiceReference reference, Map<String, ?> properties) {
		Collection<ExporterService> exporters = getExporters();
		Collection<ExportRegistration> registrations = new HashSet<ExportRegistration>(); 
		
		for (ExporterService exporter : exporters) {
			ExportRegistration reg = exporter.exportService(reference, properties);
			if (reg!=null){
				registrations.add(reg);
			}
		}

		return registrations;
	}

    @Deprecated
	public ImportRegistration importService(EndpointDescription endpoint) {
		Iterator<ImporterService> iterator = getImporters().iterator();
		
		ImportRegistration registration = null;
        ImporterService importer;

		//First successful import is the winner :P
		while (iterator.hasNext() && registration==null) {
            importer = iterator.next();
            if (!Collections.disjoint(endpoint.getConfigurationTypes(),importer.getConfigPrefix())){
			    registration = iterator.next().importService(endpoint,null);
            }
		}
		
		return registration;
	}

	public Collection<ExportReference> getExportedServices() {
		Collection<ExporterService> exporters = getExporters();
		Collection<ExportReference> refs = new HashSet<ExportReference>();
		
		for (ExporterService exporter : exporters) {
			refs.addAll(exporter.getAllExportReference());
		}
		
		return refs;
	}

	public Collection<ImportReference> getImportedEndpoints() {
		Collection<ImportReference> refs = new HashSet<ImportReference>();
		Set<ImporterService> importers = getImporters();
		
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
	
	@Property(name=RoSe_MACHINE_HOST)
	private String myhost;
	
	@Property(name=RoSe_MACHINE_ID)
	private String myid;
	
	
	/**
	 * Initialize the Machine properties thanks to the framework properties.
	 */
	private void initProperties(BundleContext context){
		
		// Initialize machineID
		if (myid==null){
			if (context.getProperty(RoSe_MACHINE_ID) != null) {
				myid = context.getProperty(RoSe_MACHINE_ID);
			} else if (context.getProperty(ENDPOINT_FRAMEWORK_UUID) != null) {
				myid = context.getProperty(ENDPOINT_FRAMEWORK_UUID);
			} else {
				myid = UUID.randomUUID().toString();
			}
		}
		
		
		// Initialize machineHost
		if (myhost==null){
			if (context.getProperty(RoSe_MACHINE_HOST) != null) {
				myhost = context.getProperty(RoSe_MACHINE_HOST);
			} else {
				myhost = DEFAULT_HOST;
			}
		}

		properties.put(RoSe_MACHINE_ID, myid);
		properties.put(SERVICE_PID, myhost+"-"+myid);
		properties.put(SERVICE_VENDOR, "org.ow2.chameleon");
		properties.put(SERVICE_RANKING, MAX_VALUE);
		properties.put(RoSe_MACHINE_HOST, myhost);
        properties.put(RoSe_MACHINE_DATE,new Date().getTime());
	}

    /**
     * {@link org.ow2.chameleon.rose.RoseMachine#getDiscoveredEndpoints()}
     */
    public Set<EndpointDescription> getDiscoveredEndpoints() {
        return importReg.getEndpoints();
    }

    /**
	 * @return This rose machine id.
	 */
	public final String getId() {
		return (String) properties.get(RoSe_MACHINE_ID);
	}

	/**
	 * @return This rose machine host.
	 */
	public final String getHost() {
		return (String) properties.get(RoSe_MACHINE_HOST);
	}

	/**
	 * @return This RoSe machine properties.
	 */
	public final Map<String, Object>  getProperties(){
		return Collections.unmodifiableMap(properties);
	}

    /**
     * @see org.ow2.chameleon.rose.RoseMachine#getExporters()
     */
    public Set<ExporterService> getExporters() {
        try{
            ServiceReference[] refs = context.getServiceReferences(ExporterService.class.getName(),"("+Constants.SERVICE_ID+"=*)");

            if (refs == null) {return Collections.emptySet();} //no exporter
            Set<ExporterService> exporters = new HashSet<ExporterService>();

            ExporterService exporter;
            for (ServiceReference ref:refs){
                exporter = (ExporterService) context.getService(ref);

                //check if the ExporterService is linked to this RoseMachine
                if (exporter.getRoseMachine() == this) {
                    exporters.add(exporter);
                }

                context.ungetService(ref);
            }

            return exporters;

        }catch (Exception e){
            log(LogService.LOG_ERROR,"Cannot get ExporterService!",e);
            return Collections.emptySet();
        }
    }

    /**
     * @see org.ow2.chameleon.rose.RoseMachine#getImporters()
     */
    public Set<ImporterService> getImporters() {
        try{
            ServiceReference[] refs = context.getServiceReferences(ImporterService.class.getName(),"("+Constants.SERVICE_ID+"=*)");

            if (refs == null) {return Collections.emptySet();} //no exporter
            Set<ImporterService> importers = new HashSet<ImporterService>();

            ImporterService importer;
            for (ServiceReference ref:refs){
                importer = (ImporterService) context.getService(ref);

                //Check if the ImporterService is linked to this RoseMachine
                if(importer.getRoseMachine() == this){
                    importers.add(importer);
                }
                context.ungetService(ref);
            }

            return importers;

        }catch (Exception e){
            log(LogService.LOG_ERROR,"Cannot get ImporterService!",e);
            return Collections.emptySet();
        }
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
