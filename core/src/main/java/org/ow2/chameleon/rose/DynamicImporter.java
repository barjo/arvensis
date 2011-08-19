package org.ow2.chameleon.rose;

import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.FrameworkUtil.createFilter;
import static org.ow2.chameleon.rose.ExporterService.ENDPOINT_CONFIG_PREFIX;
import static org.ow2.chameleon.rose.RoseMachine.ENDPOINT_LISTENER_INTEREST;
import static org.ow2.chameleon.rose.RoseMachine.EndpointListerInterrest.REMOTE;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.osgi.framework.BundleContext;
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

/**
 * A {@link DynamicImporter} allows to import all services matching a given filter with all available {@link ImporterService} dynamically.
 * Basically, a proxy is created for each EndpointDescription matching the given filter which are available through the RoseMachine for each {@link ImporterService} available.
 * If the EndpointDescription is no more available then its proxy is destroyed.
 *  
 * @author barjo
 */
public class DynamicImporter {
	private static final String DEFAULT_IMPORTER_FILTER = "(" + OBJECTCLASS
			+ "=" + ImporterService.class.getName() + ")";

	private final ImporterTracker imptracker;
	private final BundleContext context;
	private final Filter edfilter;
	private final Filter ifilter;
	private final Map<String, Object> extraProperties;
	private final DynamicImporterCustomizer customizer;


	private DynamicImporter(Builder builder) {
		extraProperties = builder.extraProperties;
		context = builder.context;
		edfilter = builder.dfilter;
		ifilter = builder.imfilter;
		customizer = builder.customizer;
		
		imptracker = new ImporterTracker();
	}

	/**
	 * Start the dynamic exporter.
	 */
	public void start() {
		imptracker.open();
	}

	/**
	 * Stop the dynamic exporter.
	 */
	public void stop() {
		imptracker.close();
	}
	
	/**
	 * @return The {@link ImportReference} created through this {@link DynamicImporter}.
	 */
	public ImportReference[] getImportReferences(){
		return customizer.getImportReferences();
	}
	

	/**
	 * Convenient Builder for the creation of a {@link DynamicImporter}.
	 * 
	 * @author barjo
	 */
	public static class Builder {
		// required
		private final BundleContext context;
		private final Filter dfilter;

		// optional
		private Filter imfilter = createFilter(DEFAULT_IMPORTER_FILTER);
		private DynamicImporterCustomizer customizer = new DefautCustomizer();
		private Map<String, Object> extraProperties = new HashMap<String, Object>();


		public Builder(BundleContext pContext, String descriptionFilter)
				throws InvalidSyntaxException {
			dfilter = createFilter(descriptionFilter);
			context = pContext;
		}
		
		public Builder protocol(List<String> protocols) throws InvalidSyntaxException{
			StringBuilder sb = new StringBuilder("(&");
			sb.append(imfilter.toString());
			sb.append("(|");
			for (String string : protocols) {
				sb.append("(");
				sb.append(ENDPOINT_CONFIG_PREFIX);
				sb.append("=");
				sb.append(string);
				sb.append(")");
			}
			sb.append("))");
			imfilter = createFilter(sb.toString());
			return this;
		}

		public Builder importerFilter(String val) throws InvalidSyntaxException {
			StringBuilder sb = new StringBuilder("(&");
			sb.append(imfilter.toString());
			sb.append(val);
			sb.append(")");
			imfilter = createFilter(sb.toString());
			
			return this;
		}

		public Builder extraProperties(Map<String, Object> val) {
			extraProperties.putAll(val);
			return this;
		}

		public Builder customizer(DynamicImporterCustomizer val) {
			customizer = val;
			return this;
		}

		public DynamicImporter build() {
			return new DynamicImporter(this);
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
			tracker = new ServiceTracker(context, ifilter, this);
		}

		private void open() {
			tracker.open();
		}

		private void close() {
			tracker.close();
		}
		
		public Object addingService(ServiceReference reference) {
			ImporterService exporter = (ImporterService) context
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
			sreg = context.registerService(EndpointListener.class.getName(), this, props);
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
	 * Default {@link DynamicExporterCustomizer}.
	 * 
	 * @author barjo
	 */
	private static class DefautCustomizer implements DynamicImporterCustomizer {
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
