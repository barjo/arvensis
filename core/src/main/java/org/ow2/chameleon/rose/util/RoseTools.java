package org.ow2.chameleon.rose.util;

import org.osgi.framework.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.RoseMachine.EndpointListerInterrest;
import org.ow2.chameleon.rose.api.InConnection;
import org.ow2.chameleon.rose.api.Instance;
import org.ow2.chameleon.rose.api.Machine;
import org.ow2.chameleon.rose.api.OutConnection;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.osgi.framework.Constants.SERVICE_ID;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.*;
import static org.ow2.chameleon.rose.RoSeConstants.ENDPOINT_CONFIG;
import static org.ow2.chameleon.rose.RoseMachine.ENDPOINT_LISTENER_INTEREST;
import static org.ow2.chameleon.rose.RoseMachine.EndpointListerInterrest.ALL;

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

		while (name == null & configs != null && i < configs.size()) {
			name = sref.getProperty(configs.get(i++) + ENDPOINT_ID);
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
			ServiceReference sref, Map<String, Object> extraProps,
			List<String> configPrefix, String machineId) {
		Map<String, Object> properties = new HashMap<String, Object>();

		if (extraProps != null) { // Add given properties
			properties.putAll(extraProps);
		}

		// Set the SERVICE_IMPORTED_CONFIGS property
		properties.put(SERVICE_IMPORTED_CONFIGS, configPrefix);

		// Set the ENDPOINT_ID property
		properties.put(ENDPOINT_ID, computeEndpointId(sref, configPrefix));
		
		// Set the Framework uuid
		properties.put(ENDPOINT_FRAMEWORK_UUID, machineId);

		return properties;
	}

	/**
	 * Return a {@link Dictionary} representation of the
	 * {@link EndpointDescription}.
	 * 
	 * @param enddesc
	 * @return a {@link Dictionary} representation of the
	 *         {@link EndpointDescription}.
	 */
	public static Dictionary<String, Object> endDescToDico(
			EndpointDescription enddesc) {
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
	public static EndpointListerInterrest getEndpointListenerInterrest(
			ServiceReference reference) {
		Object ointerrest = reference.getProperty(ENDPOINT_LISTENER_INTEREST);
		EndpointListerInterrest interrest = null;

		// Parse the ENDPOINT_LISTENER_INTERRET property
		if (ointerrest instanceof EndpointListerInterrest) {
			interrest = (EndpointListerInterrest) ointerrest;
		} else if (ointerrest instanceof String) {
			interrest = EndpointListerInterrest.valueOf((String) ointerrest);
		} else if (ointerrest == null) {
			interrest = ALL;
		} else {
			throw new IllegalArgumentException(
					"The ENDPOINT_LISTENER_INTEREST property is neither an EndpointListerInterrest or a String object.");
		}

		return interrest;
	}


	/**
	 * @param context
	 *            {@link BundleContext}
	 * @return A Snapshot of All {@link ImporterService} available within this
	 *         Machine.
	 */
	public static List<ImporterService> getAllImporter(BundleContext context) {
		try {
			return getAllImporter(context, "("
					+ ENDPOINT_CONFIG + "=*)");
		} catch (InvalidSyntaxException e) {
			assert false; // What would Dr. Gordon Freeman do ?
		}

		return emptyList();
	}

	/**
	 * @param context
	 *            {@link BundleContext}
	 * @param filter
	 * @return A Snapshot of All {@link ImporterService} available within this
	 *         Machine which match <code>filter</code>.
	 * @throws InvalidSyntaxException
	 *             if <code>filter</code> is not valid.
	 */
	public static List<ImporterService> getAllImporter(BundleContext context,
			String filter) throws InvalidSyntaxException {
		List<ImporterService> importers = new ArrayList<ImporterService>();

		ServiceReference[] srefs = context.getAllServiceReferences(
				ImporterService.class.getName(), filter);

		for (ServiceReference sref : srefs) {
			importers.add((ImporterService) context.getService(sref));
			context.ungetService(sref);
		}

		return importers;
	}

	/**
	 * Register <code>proxy</code> as a service providing the interface
	 * specified within the <code>description</code> thanks to
	 * <code>context</code>. All properties of <code>description</code> and
	 * <code>properties</code> are provided as the service properties.
	 * Properties within <code>description</code> override the one given by the
	 * <code>properties</code> parameter.
	 * 
	 * @throws IllegalArgumentException if there is any pb during the registration.
	 * @param context {@link BundleContext}
	 * @param proxy service instance.
	 * @param description {@link EndpointDescription} of which <code>proxy</code> is a client.
	 * @param properties optional properties, <code>null</code> is valid.
	 * @return {@link ServiceRegistration} of <code>proxy</code> if success
	 */
	public static ServiceRegistration registerProxy(BundleContext context,
			Object proxy, EndpointDescription description,
			Map<String, Object> properties) {
		Hashtable<String, Object> props = new Hashtable<String, Object>();
		
		if (properties != null){
			props.putAll(properties);
		}
		
		props.putAll(description.getProperties());

		return context.registerService((String[]) description.getInterfaces()
				.toArray(), proxy, props);
	}

	/**
	 * Try to load the {@link EndpointDescription} interfaces from the
	 * {@link PackageAdmin} of the gateway.
	 * 
	 * FIXME throw exception rather than returning null
	 * 
	 * @param context
	 *            The {@link BundleContext} from which we get the
	 *            {@link PackageAdmin}
	 * @param description
	 *            The {@link EndpointDescription}
	 * @return null if all specified interfaces cannot be load or the interface
	 *         {@link List}.
	 */
	public static List<Class<?>> loadClass(BundleContext context,
			EndpointDescription description) {
		ServiceReference sref = context.getServiceReference(PackageAdmin.class
				.getName());
		List<String> interfaces = description.getInterfaces();

		List<Class<?>> klass = new ArrayList<Class<?>>(interfaces.size());

		if (sref == null) { // no package admin !
			return null;
		}

		PackageAdmin padmin = (PackageAdmin) context.getService(sref);
		context.ungetService(sref);

		for (int i = 0; i < interfaces.size(); i++) {
			String itf = interfaces.get(i);
			String pname = itf.substring(0, itf.lastIndexOf(".")); // extract package name

			ExportedPackage pkg = padmin.getExportedPackage(pname);
			try {
				if (pkg.getVersion().compareTo(
						description.getPackageVersion(pname)) >= 0) {
					klass.add(pkg.getExportingBundle().loadClass(itf));
				}
			} catch (ClassNotFoundException e) {
				continue;// XXX return null or continue
			}
		}

		if (klass.isEmpty() || klass.size() < interfaces.size()) {
			return null;
		}

		return klass;
	}

    /**
     * Load the Class of name <code>klassname</code>
     * TODO handle class version
     * @param context The BundleContext
     * @param klassname The Class name.
     * @return The Class of name <code>klassname</code>
     * @throws ClassNotFoundException if we can't load the Class of name <code>klassname</code>
     */
    public static Class<?> loadClass(BundleContext context,String klassname) throws ClassNotFoundException {
        ServiceReference sref = context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin padmin = (PackageAdmin) context.getService(sref);
        String pname = klassname.substring(0, klassname.lastIndexOf(".")); // extract package name
        ExportedPackage pkg = padmin.getExportedPackage(pname);

        context.ungetService(sref);
        return pkg.getExportingBundle().loadClass(klassname);
    }
	
	/**
     * Subtracts all elements in the second list from the first list,
     * placing the results in a new list.
     * <p>
     * This differs from {@link List#removeAll(Collection)} in that
     * cardinality is respected; if <Code>list1</Code> contains two
     * occurrences of <Code>null</Code> and <Code>list2</Code> only
     * contains one occurrence, then the returned list will still contain
     * one occurrence.
     *
     * @param list1  the list to subtract from
     * @param list2  the list to subtract
     * @return  a new list containing the results
     * @throws NullPointerException if either list is null
     */
    public static <T> List<T> listSubtract(final List<T> list1, final List<T> list2) {
        final List<T> result = new ArrayList<T>(list1);
        result.removeAll(list2);

        return result;
    }

    public static void updateMachine(Machine machine,Collection<OutConnection> outs,Collection<InConnection> ins, Collection<Instance> instances){
        for(InConnection in : ins)
            in.update(machine);

        for(OutConnection out : outs)
            out.update(machine);

        for(Instance instance : instances)
            instance.update(machine);
    }

    public static void removeFromMachine(Machine machine,Collection<OutConnection> outs,Collection<InConnection> ins, Collection<Instance> instances){
        for(OutConnection out : outs)
            machine.remove(out);

        for(InConnection in : ins)
            machine.remove(in);

        for(Instance instance : instances)
            machine.remove(instance);
    }
}
