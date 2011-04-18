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
	private final Map<EndpointDescription, Integer> descriptions = new HashMap<EndpointDescription, Integer>();

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
			Collection<EndpointDescription> descSet = descriptions.keySet();
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
	 * @param description
	 * @return
	 */
	public void put(EndpointDescription description) {
		
		if (description == null) {
			throw new NullPointerException("The description must not be null");
		}

		synchronized (descriptions) {

			if (descriptions.containsKey(descriptions)) {
				
				Integer count = descriptions.get(description);
				count++;
				//TODO Log Warning
			} else {
				//First description
				descriptions.put(description,1);
			}

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
	 * @param desc
	 */
	public boolean remove(EndpointDescription desc) {

		synchronized (descriptions) {

			if (!descriptions.containsKey(desc)) {
				return false;
			}
			
			Integer count = descriptions.get(desc);
			
			if (count > 1 ){
				count--;  //TODO log something
			} 
			else { //last one, notify the listener

				descriptions.remove(desc);

				// Notify all the matching listener
				for (Entry<EndpointListener, String> entry : listeners
						.entrySet()) {
					String filter = entry.getValue();

					if (desc.matches(filter)) { // TODO check if null is valid
						entry.getKey().endpointRemoved(desc, filter);
					}
				}
			}
		}

		return true;
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
			for (EndpointDescription endpoint : descriptions.keySet()) {
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
			for (EndpointDescription endpoint : descriptions.keySet()) {
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
