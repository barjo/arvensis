package org.ow2.chameleon.rose.internal;

import static org.osgi.framework.FrameworkUtil.createFilter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.ow2.chameleon.rose.registry.ImportRegistry;

/**
 * {@link HashMap} based Implementations of {@link ImportRegistry}.
 * 
 * 
 * The listener are notified only on the first registration of an
 * EndpointDescrition or when all occurrence of an EndpointDescription has been
 * removed.
 * 
 * @author barjo
 */
public class ImportRegistryImpl implements
		ImportRegistry {

	/**
	 * The Set of registered {@link EndpointDescription}
	 */
	private final Map<Object, EndpointDescription> descriptions = new HashMap<Object, EndpointDescription>();
	private final Map<EndpointDescription, Integer> counter = new HashMap<EndpointDescription, Integer>();


	/**
	 * The Set of listeners {@link EndpointListener}
	 */
	private final Map<EndpointListener, String> listeners = new HashMap<EndpointListener, String>();


	/*-------------------------------*
	 * Internal life-cycle callbacks *
	 *-------------------------------*/
	
	protected void stop() {

		synchronized (descriptions) {
			Collection<EndpointDescription> descSet = counter.keySet();
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

					itls.remove(); //remove the listener
				}

				itdesc.remove(); //remove the counter
			}
			
			//Clear the descriptions (no more EndpointDescription)
			descriptions.clear();
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
			
			if (descriptions.containsKey(key) ) {
				throw new IllegalArgumentException("The key has already been associated with a description");
			}
			
			descriptions.put(key, description);
			
			Integer count = counter.get(description);
		
			if (count !=null){
				count++; //Increment the description counter
			} 
			else { // Initialize the counter
				counter.put(description, 1);

				// Notify all the matching listener
				for (Entry<EndpointListener, String> entry : listeners
						.entrySet()) {
					String filter = entry.getValue();

					if (description.matches(filter)) { // TODO null is valid ?
						entry.getKey().endpointAdded(description, filter);
					}
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

			desc = descriptions.get(key);

			if (desc != null) { // This description is still available
				Integer count = counter.get(desc);
				
				if (--count < 1) { // All description has been removed
					desc = descriptions.remove(key);
					counter.remove(desc);

					// Notify all the matching listener
					for (Entry<EndpointListener, String> entry : listeners
							.entrySet()) {
						String filter = entry.getValue();

						if (desc.matches(filter)) { // TODO check if null valid
							entry.getKey().endpointRemoved(desc, filter);
						}
					}
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
			for (EndpointDescription endpoint : counter.keySet()) {
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
		
		if (filter != null){
			createFilter(filter);
		}
		
		synchronized (descriptions) {
			listeners.put(listener, filter);
			for (EndpointDescription endpoint : counter.keySet()) {
				if (filter == null || endpoint.matches(filter)) {
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
			if (listeners.containsKey(listener)){
				String filter = listeners.remove(listener);
				
				for (EndpointDescription endpoint : counter.keySet()) {
					if (filter == null || endpoint.matches(filter)) {
						listener.endpointRemoved(endpoint, filter);
					}
				}
			}
		}
	}

	public boolean contains(EndpointDescription desc) {
		synchronized (descriptions) {
			return counter.containsKey(desc);
		}
	}
}
