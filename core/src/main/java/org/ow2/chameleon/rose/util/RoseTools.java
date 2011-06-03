package org.ow2.chameleon.rose.util;

import static java.util.Collections.emptyList;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;
import static org.ow2.chameleon.rose.ExporterService.ENDPOINT_CONFIG_PREFIX;
import static org.ow2.chameleon.rose.RoseMachine.ENDPOINT_LISTENER_INTEREST;
import static org.ow2.chameleon.rose.RoseMachine.EndpointListerInterrest.ALL;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.ow2.chameleon.rose.ExporterService;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.RoseMachine.EndpointListerInterrest;

/**
 * This class contains some useful static methods.
 * 
 * @author barjo
 */
public final class RoseTools {

	/**
	 * Get the endpoint id from the {@link ServiceReference}. If the name
	 * property if not set, use the {@link RemoteConstants#ENDPOINT_ID} property
	 * or the {@link Constants#SERVICE_PID} or the {@link Constants#SERVICE_ID}
	 * + <code>service</code> as a prefix.
	 * 
	 * @param sref
	 * @param configs
	 * @return {@link String} the endpoint id.
	 */
	public static String computeEndpointId(final ServiceReference sref,
			final List<String> configs) {
		Object name = sref.getProperty(ENDPOINT_ID);

		// get the endpoint name from the given name properties
		int i = 0;
		
		while (name == null & configs!=null && i < configs.size()) {
			name = sref.getProperty(configs.get(i++)+ ENDPOINT_ID);
		}

		// try with instance.name
		if (name == null) {
			name = sref.getProperty("instance.name");
		}

		// try with service.pid
		if (name == null) {
			name = sref.getProperty(SERVICE_PID);
		}

		// try with service.id
		if (name == null) {
			name = "service" + String.valueOf(sref.getProperty(SERVICE_ID));
		}

		return String.valueOf(name);
	}

	/**
	 * Compute some {@link EndpointDescription} extra properties from
	 * <code>sref</code>, <code>extraProps</code> and <code>configPrefix</code>.
	 * 
	 * @param sref
	 * @param extraProps
	 * @param configPrefix
	 *            Configuration prefix (e.g. <code>jsonrpc,org.jabsorb</code>
	 * @return {@link Map} containing the extra properties.
	 */
	public static Map<String, Object> computeEndpointExtraProperties(
			ServiceReference sref, Map<String, Object> extraProps, List<String> configPrefix) {
		Map<String, Object> properties = new HashMap<String, Object>();

		if (extraProps != null) { // Add given properties
			properties.putAll(extraProps);
		}

		//Set the SERVICE_IMPORTED_CONFIGS property
		properties.put(SERVICE_IMPORTED_CONFIGS, configPrefix);
		
		//Set the ENDPOINT_ID property
		properties.put(ENDPOINT_ID, computeEndpointId(sref, configPrefix));
		
		return properties;
	}
	
	/**
	 * Return a {@link Dictionary} representation of the {@link EndpointDescription}.
	 * @param enddesc
	 * @return a {@link Dictionary} representation of the {@link EndpointDescription}.
	 */
	public static Dictionary<String, Object> endDescToDico(EndpointDescription enddesc){
		return new Hashtable<String, Object>(enddesc.getProperties());
	}

	/**
	 * @param reference
	 *            The {@link ServiceReference} of an {@link EndpointListener}.
	 * @return {@link EndpointListerInterrest} of the given
	 *         {@link ServiceReference} or <code>ALL</code> if it has not been
	 *         set.
	 * @throws IllegalArgumentException
	 *             if the property
	 *             {@link RoseMachine#ENDPOINT_LISTENER_INTEREST} is not a valid
	 *             String or and {@link EndpointListerInterrest}.
	 */
	public static EndpointListerInterrest getEndpointListenerInterrest(ServiceReference reference){
		Object ointerrest = reference.getProperty(ENDPOINT_LISTENER_INTEREST);
		EndpointListerInterrest interrest = null;

		// Parse the ENDPOINT_LISTENER_INTERRET property
		if (ointerrest instanceof EndpointListerInterrest) {
			interrest = (EndpointListerInterrest) ointerrest;
		} else if (ointerrest instanceof String) {
			interrest = EndpointListerInterrest.valueOf((String) ointerrest);
		} else if (ointerrest == null){
			interrest = ALL;
		} else {
			throw new IllegalArgumentException(
					"The ENDPOINT_LISTENER_INTEREST property is neither an EndpointListerInterrest or a String object.");
		}
		
		return interrest;
	}
	
	/**
	 * @param context {@link BundleContext}
	 * @return A Snapshot of All {@link ExporterService} available within this Machine.
	 */
	public static List<ExporterService> getAllExporter(BundleContext context){
		
		try {
			return getAllExporter(context, "("+ENDPOINT_CONFIG_PREFIX+"=*)");
		} catch (InvalidSyntaxException e) {
			assert false; //What would Dr. Gordon Freeman do ?
		}
		
		return emptyList();
	}
	
	/**
	 * @param context {@link BundleContext}
	 * @param filter
	 * @return A Snapshot of All {@link ExporterService} available within this Machine which match <code>filter</code>.
	 * @throws InvalidSyntaxException if <code>filter</code> is not valid.
	 */
	public static List<ExporterService> getAllExporter(BundleContext context,String filter) throws InvalidSyntaxException{
		List<ExporterService> exporters = new ArrayList<ExporterService>();
		
		ServiceReference[] srefs = context.getAllServiceReferences(
				ExporterService.class.getName(), filter);

		for (ServiceReference sref : srefs) {
			exporters.add((ExporterService) context.getService(sref));
			context.ungetService(sref);
		}
			
		
		return exporters;
	}
	
	/**
	 * @param context {@link BundleContext}
	 * @return A Snapshot of All {@link ImporterService} available within this Machine.
	 */
	public static List<ImporterService> getAllImporter(BundleContext context){
		try {
			return getAllImporter(context, "("+ImporterService.ENDPOINT_CONFIG_PREFIX+"=*)");
		} catch (InvalidSyntaxException e) {
			assert false; //What would Dr. Gordon Freeman do ?
		}
		
		return emptyList();
	}
	
	/**
	 * @param context {@link BundleContext}
	 * @param filter
	 * @return A Snapshot of All {@link ImporterService} available within this Machine which match <code>filter</code>.
	 * @throws InvalidSyntaxException if <code>filter</code> is not valid.
	 */
	public static List<ImporterService> getAllImporter(BundleContext context,String filter) throws InvalidSyntaxException{
		List<ImporterService> importers = new ArrayList<ImporterService>();
		
		ServiceReference[] srefs = context.getAllServiceReferences(
				ImporterService.class.getName(), filter);

		for (ServiceReference sref : srefs) {
			importers.add((ImporterService) context.getService(sref));
			context.ungetService(sref);
		}
			
		
		return importers;
	}
	
}
