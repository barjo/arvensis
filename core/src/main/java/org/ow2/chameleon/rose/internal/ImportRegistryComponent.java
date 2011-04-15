package org.ow2.chameleon.rose.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.ow2.chameleon.rose.registry.ImportRegistryListening;
import org.ow2.chameleon.rose.registry.ImportRegistryProvisioning;
import org.ow2.chameleon.rose.registry.ImportRegistryService;

/**
 * ImportRegistry {@link ComponentFactory} which provides an
 * {@link ImportRegistryListening} service and a {@link ImportRegistryProvisioning} service.
 * 
 * @author barjo
 */
@Component(name="rose.import.registry",immediate=true)
@Provides(specifications={ImportRegistryListening.class,ImportRegistryProvisioning.class})
@Instantiate(name="rose.import.registry-instance")
public class ImportRegistryComponent implements
		ImportRegistryService {

	/**
	 * The Set of registered {@link EndpointDescription}
	 */
	private final Map<Object, EndpointDescription> descriptions = new HashMap<Object, EndpointDescription>();

	/**
	 * The Set of listeners {@link EndpointListener}
	 */
	private final Map<EndpointListener, String> listeners = new HashMap<EndpointListener, String>();


	/*-------------------------------*
	 * Internal life-cycle callbacks *
	 *-------------------------------*/
	
	@Invalidate
	@SuppressWarnings("unused")
	private void stop() {

		synchronized (descriptions) {
			Collection<EndpointDescription> descSet = descriptions.values();
			Collection<Entry<EndpointListener, String>> lsEnrtySet = listeners
					.entrySet();

			//notify all listeners of the endpoints unavailability
			for (Iterator<EndpointDescription> itdesc = descSet.iterator(); itdesc
					.hasNext();) {
				EndpointDescription desc = itdesc.next();

				for (Iterator<Entry<EndpointListener, String>> itls = lsEnrtySet
						.iterator(); itls.hasNext();) {
					Entry<EndpointListener, String> entry = itls.next();
					entry.getKey().endpointRemoved(desc, entry.getValue());

					itls.remove();
				}

				itdesc.remove();
			}
		}
	}
	
	/*-------------------------------------*
	 * ImportRegistryProvisioning service  *
	 *-------------------------------------*/
	
	/**
	 * Add an {@link EndpointDescription}.All {@link EndpointListener} which are
	 * interesting in the {@link EndpointDescription} are notified.
	 * 
	 * @param key
	 * @param description
	 * @return
	 * @throw {@link IllegalArgumentException} if the
	 *        {@link EndpointDescription} has already been added.
	 */
	public void put(Object key,
			EndpointDescription description) {
		
		if (key == null || description == null) {
			throw new NullPointerException("The description and the key must not be null");
		}

		synchronized (descriptions) {

			if (descriptions.containsKey(key) || descriptions.containsValue(description)) {
				throw new IllegalArgumentException("The key has already been associated with a description, or vice-versa");
			}

			descriptions.put(key, description);

			// Notify all the matching listener
			for (Entry<EndpointListener, String> entry : listeners.entrySet()) {
				String filter = entry.getValue();

				if (description.matches(filter)) { // TODO check if null is a
													// valid value
					entry.getKey().endpointAdded(description, filter);
				}
			}
		}
	}

	/**
	 * Remove an {@link EndpointDescription}. All {@link EndpointListener} which
	 * are interesting in the {@link EndpointDescription} are notified.
	 * 
	 * @param key
	 * @return The previous value associated with the <code>key</code> or
	 *         <code>null</code> if there is no mapping for the key.
	 */
	public EndpointDescription remove(Object key) {
		EndpointDescription desc;

		synchronized (descriptions) {

			if (!descriptions.containsKey(key)) {
				return null;
			}

			desc = descriptions.remove(key);

			// Notify all the matching listener
			for (Entry<EndpointListener, String> entry : listeners.entrySet()) {
				String filter = entry.getValue();

				if (desc.matches(filter)) { // TODO check if null is a valid
											// value
					entry.getKey().endpointRemoved(desc, filter);
				}
			}

		}

		return desc;
	}
	

	/*-------------------------------------*
	 *   ImportRegistryListening service   *
	 *-------------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.registry.ImportRegistryListening#addEndpointListener(org.osgi.service.remoteserviceadmin.EndpointListener)
	 */
	public void addEndpointListener(EndpointListener listener) {
		synchronized (descriptions) {
			listeners.put(listener, null);
			for (EndpointDescription endpoint : descriptions.values()) {
				listener.endpointAdded(endpoint, null);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.registry.ImportRegistryListening#addEndpointListener(org.osgi.service.remoteserviceadmin.EndpointListener, java.lang.String)
	 */
	public void addEndpointListener(EndpointListener listener, String filter)
			throws InvalidSyntaxException {
		FrameworkUtil.createFilter(filter);

		synchronized (descriptions) {
			listeners.put(listener, null);
			for (EndpointDescription endpoint : descriptions.values()) {
				if (endpoint.matches(filter)) {
					listener.endpointAdded(endpoint, filter);
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * @see org.ow2.chameleon.rose.registry.ImportRegistryListening#removeEndpointListener(org.osgi.service.remoteserviceadmin.EndpointListener)
	 */
	public void removeEndpointListener(EndpointListener listener) {
		synchronized (descriptions) {
			listeners.remove(listener);
			// XXX Should we called listener.endpointRemoved ??
		}

	}

}
