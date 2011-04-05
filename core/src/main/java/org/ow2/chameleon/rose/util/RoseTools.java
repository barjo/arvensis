package org.ow2.chameleon.rose.util;

import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_ID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_IMPORTED_CONFIGS;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

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
	
}
