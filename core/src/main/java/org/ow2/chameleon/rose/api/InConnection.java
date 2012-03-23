package org.ow2.chameleon.rose.api;

import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.FrameworkUtil.createFilter;
import static org.ow2.chameleon.rose.RoSeConstants.ENDPOINT_CONFIG;
import static org.ow2.chameleon.rose.RoseMachine.ENDPOINT_LISTENER_INTEREST;
import static org.ow2.chameleon.rose.RoseMachine.EndpointListerInterrest.REMOTE;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.ImporterService;

/**
 * A {@link InConnection} allows to import all services matching a given filter with all available {@link ImporterService} dynamically.
 * Basically, a proxy is created for each EndpointDescription matching the given filter which are available through the RoseMachine for each {@link ImporterService} available.
 * If the EndpointDescription is no more available then its proxy is destroyed.
 *  
 * @author barjo
 */
public final class InConnection {
	private static final String DEFAULT_IMPORTER_FILTER = "(" + OBJECTCLASS
			+ "=" + ImporterService.class.getName() + ")";

	private final ImporterTracker imptracker;
	private final Machine machine;
	private final Filter edfilter;
	private final Filter ifilter;
	private final Map<String, Object> extraProperties;
	private final InCustomizer customizer;


	private InConnection(InBuilder builder) {
		extraProperties = builder.extraProperties;
		machine = builder.machine;
		edfilter = builder.dfilter;
		ifilter = builder.imfilter;
		customizer = builder.customizer;
		machine.add(this);
		imptracker = new ImporterTracker();
	}

	/**
	 * Start the dynamic exporter.
	 */
	public void open() {
		imptracker.open();
	}

	/**
	 * Stop the dynamic exporter.
	 */
	public void close() {
		imptracker.close();
	}
	
	/**
	 * @return The {@link ImportReference} created through this {@link InConnection}.
	 */
	public ImportReference[] getImportReferences(){
		return customizer.getImportReferences();
	}
	

	/**
	 * Convenient Builder for the creation of a {@link InConnection}.
	 * 
	 * @author barjo
	 */
	public static class InBuilder {
		// required
		private final Machine machine;
		private final Filter dfilter;

		// optional
		private Filter imfilter = createFilter(DEFAULT_IMPORTER_FILTER);
		private InCustomizer customizer = new DefautCustomizer();
		private Map<String, Object> extraProperties = new HashMap<String, Object>();


		private InBuilder(Machine pMachine, String descriptionFilter)
				throws InvalidSyntaxException {
			dfilter = createFilter(descriptionFilter);
			machine = pMachine;
		}
		
		public static InBuilder in(Machine pMachine, String descriptionFilter)
				throws InvalidSyntaxException {
			return new InBuilder(pMachine, descriptionFilter);
		}
		
		public InBuilder protocol(List<String> protocols) throws InvalidSyntaxException{
			StringBuilder sb = new StringBuilder("(&");
			sb.append(imfilter.toString());
			sb.append("(|");
			for (String string : protocols) {
				sb.append("(");
				sb.append(ENDPOINT_CONFIG);
				sb.append("=");
				sb.append(string);
				sb.append(")");
			}
			sb.append("))");
			imfilter = createFilter(sb.toString());
			return this;
		}

		public InBuilder importerFilter(String val) throws InvalidSyntaxException {
			StringBuilder sb = new StringBuilder("(&");
			sb.append(imfilter.toString());
			sb.append(val);
			sb.append(")");
			imfilter = createFilter(sb.toString());
			
			return this;
		}
		
		public InBuilder property(String key, Object value){
			extraProperties.put(key, value);
			return this;
		}

		public InBuilder extraProperties(Map<String, Object> val) {
			extraProperties.putAll(val);
			return this;
		}

		public InBuilder customizer(InCustomizer val) {
			customizer = val;
			return this;
		}

		public InConnection create() {
			return new InConnection(this);
		}
	}

	/**
	 * Track All {@link ExporterService} matching <code>xfilter</code> and
	 * create a {@link ServiceToBeImporterTracker} tracker for each of them.
	 * 
	 * @author barjo
	 */
	private class ImporterTracker implements ServiceTrackerCustomizer {
		private final ServiceTracker tracker;

		private ImporterTracker() {
			tracker = new ServiceTracker(machine.getContext(), ifilter, this);
		}

		private void open() {
			tracker.open();
		}

		private void close() {
			tracker.close();
		}
		
		public Object addingService(ServiceReference reference) {
			ImporterService exporter = (ImporterService) machine.getContext()
					.getService(reference);
			return new ServiceToBeImporterTracker(exporter);
		}

		public void modifiedService(ServiceReference reference, Object object) {
			// nothing to do

		}

		public void removedService(ServiceReference reference, Object object) {
			ServiceToBeImporterTracker stracker = (ServiceToBeImporterTracker) object;
			stracker.close(); //close the tracker
		}
	}

	/**
	 * Track All EndpointDescription matching <code>sfilter</code> and import them with the
	 * {@link ImporterService} given in the constructor
	 * 
	 * @author barjo
	 */
	private class ServiceToBeImporterTracker implements
			EndpointListener {
		private final ImporterService importer;
		private final ServiceRegistration sreg;
		private final Hashtable<String, Object> props;
		private final Map<EndpointDescription, Object> tracked;

		private ServiceToBeImporterTracker(ImporterService pImporter) {
			//Track only the remote EndpointDescription
			props = new Hashtable<String, Object>();
			props.put(ENDPOINT_LISTENER_INTEREST, REMOTE);
			props.put(ENDPOINT_LISTENER_SCOPE, edfilter);
			
			//The tracked description 
			tracked = new HashMap<EndpointDescription, Object>();
			
			importer = pImporter;
			sreg = machine.getContext().registerService(EndpointListener.class.getName(), this, props);
		}

		private void close() {
			sreg.unregister();
		}


		public void endpointAdded(EndpointDescription endpoint,
				String matchedFilter) {
			Object key = customizer.doImport(importer, endpoint,extraProperties);
			if (key != null){
				tracked.put(endpoint, key);
			}
			
		}

		public void endpointRemoved(EndpointDescription endpoint,
				String matchedFilter) {
			Object key = tracked.remove(endpoint);
			if(key!=null){
				customizer.unImport(importer, endpoint, key);
			}
		}
	}

	/**
	 * Default {@link OutCustomizer}.
	 * 
	 * @author barjo
	 */
	private static class DefautCustomizer implements InCustomizer {
		private final ConcurrentLinkedQueue<ImportReference> irefs = new ConcurrentLinkedQueue<ImportReference>();
		

		public ImportReference[] getImportReferences()
				throws UnsupportedOperationException {
			return (ImportReference[]) irefs.toArray();
		}

		public Object doImport(ImporterService importer,
				EndpointDescription description,Map<String, Object> properties) {
			ImportRegistration registration = importer.importService(description,properties);
			ImportReference iref = registration.getImportReference();
			if (iref!=null){
				irefs.add(registration.getImportReference());
			}//XXX else log exception ? registration.getException ?
			return registration;
		}

		public void unImport(ImporterService importer,
				EndpointDescription description, Object registration) {
			ImportRegistration regis = (ImportRegistration) registration;
			irefs.remove(regis.getImportReference());
			regis.close();
		}
	}

}
