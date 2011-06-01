package org.ow2.chameleon.rose.registry;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.ow2.chameleon.rose.internal.ExportRegistryImpl;

public interface ExportRegistryListening {
	/**
	 * Macro for {@link ExportRegistryListening#addEndpointListener(EndpointListener, String)
	 * addEndpointListener(listener, null)}
	 * 
	 * @param listener The {@link EndpointListener} object to be add.
	 */
	void addEndpointListener(EndpointListener listener);

	/**
	 * Adds the specifier {@link EndpointListener} object to the {@link ExportRegistryImpl} list
	 * of listeners. See {@link Filter} for a description of the filter syntax.
	 * {@link EndpointListener} objects are notified when an endpoint has a
	 * lifecyle state change.
	 * 
	 * If the {@link ExportRegistryImpl}'s list of listeners already contains
	 * <code>listener</code>, then this methods replaces that listner's filter
	 * with the new one.
	 * 
	 * 
	 * @param listener The {@link EndpointListener} object to be add.
	 * @param filter  A valid {@link Filter}, If <code>null</code> all
	 *            {@link EndpointDescription} are considered to match the
	 *            filter.
	 * @throws InvalidSyntaxException If the <code>filter</code> parameter is not a valid filter.
	 */
	void addEndpointListener(EndpointListener listener, String filter) throws InvalidSyntaxException;

	/**
	 * Removes the <code>listener</code> object from the {@link ExportRegistryImpl}'s list of
	 * {@link EndpointListener}.
	 * 
	 * If <code>listener</code> is not contained in this {@link ExportRegistryImpl}'s list of
	 * {@link EndpointListener}, this method does nothing.
	 * 
	 * @param listener The {@link EndpointListener} to be removed.
	 */
	void removeEndpointListener(EndpointListener listener);
}
