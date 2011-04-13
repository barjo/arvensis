package org.ow2.chameleon.rose.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.ipojo.ComponentFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;

/**
 * Abstract rregistry-listening {@link ComponentFactory} which provides an
 * {@link RRegistryListeningService} service.
 * 
 * @author barjo
 */
public abstract class AbstractRRegistryListeningComp implements
		RRegistryListeningService {

	/**
	 * The Set of registered {@link EndpointDescription}
	 */
	private final Map<Object, EndpointDescription> descriptions = new HashMap<Object, EndpointDescription>();

	/**
	 * The Set
	 */
	private final Map<EndpointListener, String> listeners = new HashMap<EndpointListener, String>();

	protected final BundleContext context; // The BundleContext

	public AbstractRRegistryListeningComp(BundleContext pContext) {
		context = pContext;
	}

	/**
	 * Add an {@link EndpointDescription}.All {@link EndpointListener} which are
	 * interesting in the {@link EndpointDescription} are notified.
	 * 
	 * @param key
	 * @param description
	 * @return
	 * @throw {@link IllegalArgumentException} if the
	 *        {@link EndpointDescription} has already been added or 
	 */
	protected EndpointDescription put(Object key,
			EndpointDescription description) {
		EndpointDescription returned;
		
		if (key == null || description == null) {
			throw new NullPointerException("The description and the key must not be null");
		}

		synchronized (descriptions) {

			if (descriptions.containsKey(key) || descriptions.containsValue(description)) {
				throw new IllegalArgumentException("The key has already been associated with a descriptio, or vice-versa");
			}

			returned = descriptions.put(key, description);

			// Notify all the matching listener
			for (Entry<EndpointListener, String> entry : listeners.entrySet()) {
				String filter = entry.getValue();

				if (description.matches(filter)) { // TODO check if null is a
													// valid value
					entry.getKey().endpointAdded(description, filter);
				}
			}
		}

		return returned;
	}

	/**
	 * Remove an {@link EndpointDescription}. All {@link EndpointListener} which
	 * are interesting in the {@link EndpointDescription} are notified.
	 * 
	 * @param key
	 * @return The previous value associated with the <code>key</code> or
	 *         <code>null</code> if there is no mapping for the key.
	 */
	protected EndpointDescription remove(Object key) {
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

	/**
	 * Clean the registry, i.e. removes all the {@link EndpointDescription} and
	 * all the {@link EndpointListener}. The {@link EndpointListener} are
	 * notified before being removed.
	 */
	protected void clean() {

		synchronized (descriptions) {
			Collection<EndpointDescription> descSet = descriptions.values();
			Collection<Entry<EndpointListener, String>> lsEnrtySet = listeners
					.entrySet();

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
	 *  RRegistryListeningService methods  *
	 *-------------------------------------*/

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ow2.chameleon.rose.registry.RRegistryListeningService#addEndpointListener
	 * (org.osgi.service.remoteserviceadmin.EndpointListener)
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
	 * 
	 * @see
	 * org.ow2.chameleon.rose.registry.RRegistryListeningService#addEndpointListener
	 * (org.osgi.service.remoteserviceadmin.EndpointListener, java.lang.String)
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
	 * 
	 * @see org.ow2.chameleon.rose.registry.RRegistryListeningService#
	 * removeEndpointListener
	 * (org.osgi.service.remoteserviceadmin.EndpointListener)
	 */
	public void removeEndpointListener(EndpointListener listener) {
		synchronized (descriptions) {
			listeners.remove(listener);
			// TODO Should we called listener.endpointRemoved ??
		}

	}

}
