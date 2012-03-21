package org.ow2.chameleon.rose;

import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.FrameworkUtil.createFilter;
import static org.ow2.chameleon.rose.RoSeConstants.ENDPOINT_CONFIG;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * A {@link OutConnection} allows to export all services matching a given
 * filter with all available {@link ExporterService} dynamically. Basically, an
 * endpoint is created for each services matching the given filter which are
 * available on the gateway for each {@link ExporterService} available. If the
 * service is no more available then all his endpoints are destroyed.
 * 
 * @author barjo
 */
public final class OutConnection {
	private static final String DEFAULT_EXPORTER_FILTER = "(" + OBJECTCLASS
			+ "=" + ExporterService.class.getName() + ")";

	private final ExporterTracker extracker;
	private final BundleContext context;
	private final Filter sfilter;
	private final Filter xfilter;
	private final Map<String, Object> extraProperties;
	private final OutCustomizer customizer;

	private OutConnection(OutBuilder builder) {
		extraProperties = builder.extraProperties;
		context = builder.context;
		sfilter = builder.sfilter;
		xfilter = builder.xfilter;
		customizer = builder.customizer;

		extracker = new ExporterTracker();
	}

	/**
	 * Start the dynamic exporter.
	 */
	public void start() {
		extracker.open();
	}

	/**
	 * Stop the dynamic exporter.
	 */
	public void stop() {
		extracker.close();
	}

	/**
	 * @return The {@link ExportReference} created through this
	 *         {@link OutConnection}.
	 */
	public ExportReference[] getExportedReference() {
		return customizer.getExportReferences();
	}

	/**
	 * Convenient Builder for the creation of a {@link OutConnection}.
	 * 
	 * @author barjo
	 */
	public static class OutBuilder {
		// required
		private final BundleContext context;
		private final Filter sfilter;

		// optional
		private Filter xfilter = createFilter(DEFAULT_EXPORTER_FILTER);
		private Map<String, Object> extraProperties = new HashMap<String, Object>();
		private OutCustomizer customizer = null;

		private OutBuilder(BundleContext pContext, String serviceFilter)
				throws InvalidSyntaxException {
			sfilter = createFilter(serviceFilter);
			context = pContext;
			
			if (customizer == null){ //Set default customizer
				customizer = new DefautCustomizer(context);
			}
		}
		
		public static OutBuilder out(BundleContext pContext, String serviceFilter) throws InvalidSyntaxException{
			return new OutBuilder(pContext, serviceFilter);
		}

		public OutBuilder protocol(List<String> protocols)
				throws InvalidSyntaxException {
			StringBuilder sb = new StringBuilder("(&");
			sb.append(xfilter.toString());
			sb.append("(|");
			for (String string : protocols) {
				sb.append("(");
				sb.append(ENDPOINT_CONFIG);
				sb.append("=");
				sb.append(string);
				sb.append(")");
			}
			sb.append("))");
			xfilter = createFilter(sb.toString());
			return this;
		}

		public OutBuilder exporterFilter(String val) throws InvalidSyntaxException {
			StringBuilder sb = new StringBuilder("(&");
			sb.append(xfilter.toString());
			sb.append(val);
			sb.append(")");
			xfilter = createFilter(sb.toString());
			return this;
		}

		public OutBuilder extraProperties(Map<String, Object> val) {
			extraProperties.putAll(val);
			return this;
		}

		public OutBuilder customizer(OutCustomizer val) {
			customizer = val;
			return this;
		}

		public OutConnection create() {
			return new OutConnection(this);
		}
	}

	/**
	 * Track All {@link ExporterService} matching <code>xfilter</code> and
	 * create a {@link ServiceToBeExportedTracker} tracker for each of them.
	 * 
	 * @author barjo
	 */
	private class ExporterTracker implements ServiceTrackerCustomizer {
		private final ServiceTracker tracker;

		private ExporterTracker() {
			tracker = new ServiceTracker(context, xfilter, this);
		}

		private void open() {
			tracker.open();
		}

		private void close() {
			tracker.close();
		}

		public Object addingService(ServiceReference reference) {
			ExporterService exporter = (ExporterService) context
					.getService(reference);
			return new ServiceToBeExportedTracker(exporter);
		}

		public void modifiedService(ServiceReference reference, Object object) {
			// nothing to do

		}

		public void removedService(ServiceReference reference, Object object) {
			ServiceToBeExportedTracker stracker = (ServiceToBeExportedTracker) object;
			stracker.close(); // close the tracker
		}
	}

	/**
	 * Track All service matching <code>sfilter</code> and export them with the
	 * {@link ExporterService} given in the constructor
	 * 
	 * @author barjo
	 */
	private class ServiceToBeExportedTracker implements
			ServiceTrackerCustomizer {
		private final ExporterService exporter;
		private final ServiceTracker tracker;

		private ServiceToBeExportedTracker(ExporterService pExporter) {
			exporter = pExporter;
			tracker = new ServiceTracker(context, sfilter, this);
			tracker.open();
		}

		private void close() {
			tracker.close();
		}

		public Object addingService(ServiceReference reference) {
			return customizer.export(exporter, reference, extraProperties);
		}

		public void modifiedService(ServiceReference reference, Object object) {
			// XXX not supported
		}

		public void removedService(ServiceReference reference, Object object) {
			customizer.unExport(exporter, reference, object);
		}
	}

	/**
	 * Default {@link OutCustomizer}.
	 * 
	 * @author barjo
	 */
	private static class DefautCustomizer implements OutCustomizer {
		private final ConcurrentLinkedQueue<ExportReference> xrefs;
		private final BundleContext context;
		
		public DefautCustomizer(BundleContext pcontext) {
			context = pcontext;
			xrefs = new ConcurrentLinkedQueue<ExportReference>();
		}

		public ExportRegistration export(ExporterService exporter,
				ServiceReference sref, Map<String, Object> properties) {
			ExportRegistration registration = exporter.exportService(sref,
					properties);
			
			if (registration.getException() == null){ //Successful export
				xrefs.add(registration.getExportReference());
			}else { //export failed
				log(LogService.LOG_WARNING, "Cannot export service of id: "
						+ sref.getProperty(SERVICE_ID)
						+ " provided by the bundle of id"
						+ sref.getBundle().getBundleId(),
						registration.getException());
			}
			
			return registration;
		}

		public void unExport(ExporterService exporter, ServiceReference sref,
				Object registration) {
			ExportRegistration reg = (ExportRegistration) registration;
			if (reg.getException() == null){ //was indeed exported
				xrefs.remove(reg.getExportReference());
			}
			reg.close();
		}

		public ExportReference[] getExportReferences() {
			return (ExportReference[]) xrefs.toArray();
		}
		
		/**
		 * Wrapper around the {@link LogService#log(int, String, Throwable)} method.
		 * 
		 * @param level The {@link LogService} log level.
		 * @param message An optional message to log
		 * @param exception The exception which need to be log.
		 */
		private void log(int level, String message, Throwable exception){
			ServiceReference sref = context.getServiceReference(LogService.class.getName());
			if (sref !=null){
				try{
					LogService logger = (LogService) context.getService(sref);
					logger.log(level,message,exception);
				}finally{
					context.ungetService(sref);
				}
			}
			//XXX sysout something if there is no LogService ?
		}
		
	}
}
