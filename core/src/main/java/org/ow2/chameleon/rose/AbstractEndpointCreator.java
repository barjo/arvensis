package org.ow2.chameleon.rose;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

public abstract class AbstractEndpointCreator implements ExporterService {
	protected final Map<ServiceReference, MyExportReference> references;
	protected final Map<ExportReference,Collection<MyExportRegistration>> registrations;
	
	private final BundleContext context;
	
	public  AbstractEndpointCreator(BundleContext pContext) {
		context=pContext;
		references = new HashMap<ServiceReference, MyExportReference>();
		registrations = new HashMap<ExportReference, Collection<MyExportRegistration>>();
	}
	
	/**
	 * Create an endpoint for the service linked to the given ServiceReference. 
	 * If the service has already an endpoint with compatible properties, 
	 * return the existing EndpointDescription, otherwise an exception must be thrown.
	 * @param sref
	 * @return The EndpointDescription of the created endpoint.
	 */
	public abstract EndpointDescription createEndpoint(ServiceReference sref,Map<String,Object> extraProperties);
	
	/**
	 * Destroy the endpoint of given EndpointDescription.
	 * @param endesc
	 */
	public abstract void destroyEndpoint(EndpointDescription endesc);
	
	/**
	 * FIXME
	 * @param sref
	 * @param extraProperties
	 * @return
	 */
	public final ExportRegistration exportService(ServiceReference sref,Map<String,Object> extraProperties) {
		final ExportRegistration xreg;
		
		if (references.containsKey(sref)) {
			xreg = new MyExportRegistration(references.get(sref), null);
		} else {
			EndpointDescription enddesc = createEndpoint(sref,extraProperties);
			ExportReference xref = new MyExportReference(sref, enddesc,context);
			xreg = new MyExportRegistration(xref, null);
		}
		
		return xreg;
	}
	
	public final Collection<ExportReference> getAllExportReference() {
		return new HashSet<ExportReference>(references.values());
	}
	
	public final void destroyAllEndpoint(){
		
	}
	
	/*--------------------------------*
	 *             PRIVATE            *
	 *--------------------------------*/
	
	/**
	 * TODO
	 */
	private void destroyExportRegistration(ExportRegistration xRegistration){
		
	}
	
	
	/**
	 * Implementation of an ExportReference.
	 * @author barjo
	 */
	private final class MyExportReference implements ExportReference {
		private ServiceReference sref;
		private EndpointDescription desc;
		private ServiceRegistration regis;
		private volatile boolean closed = false;
		
		public MyExportReference(ServiceReference pSref,EndpointDescription pEnddesc,BundleContext context) {
			sref = pSref;
			desc = pEnddesc;
			regis = context.registerService(EndpointDescription.class.getName(),pEnddesc, toDico(pEnddesc));

		}

		public synchronized ServiceReference getExportedService() {
			return sref;
		}

		public synchronized EndpointDescription getExportedEndpoint() {
			return desc;
		}
		
		public synchronized void close() {
			if (!closed) {
				regis.unregister();
				regis = null;
				sref = null;
				desc = null;
			}
		}
	}
	
	/**
	 * Implementation of an ExportRegistration.
	 * @author barjo
	 */
	private final class MyExportRegistration implements ExportRegistration {
		private ExportReference xref;
		private Throwable exception;
		
		private volatile boolean closed = false;

		
		private MyExportRegistration(ExportReference pXref,Throwable pException) {
			xref=pXref;
			exception = pException;
		}
		
		public synchronized ExportReference getExportReference() {
			return xref;
		}

		public synchronized void close() {
			if (!closed) {
				destroyExportRegistration(this);
				xref = null;
				exception = null;
			}
		}

		public synchronized Throwable getException() {
			return exception;
		}
	}
	
	private static Dictionary<String, Object> toDico(EndpointDescription enddesc){
		return new Hashtable<String, Object>(enddesc.getProperties());
	}
}
