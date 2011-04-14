package org.ow2.chameleon.rose.internal;

import static org.ow2.chameleon.rose.util.RoseTools.endDescToDico;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.ow2.chameleon.rose.registry.ExportRegistryProvisoning;

@Component(name="rose.export.registry",immediate=true)
@Instantiate(name="rose.export.registry-instance")
@Provides(specifications=ExportRegistryProvisoning.class)
public class ExportRegistryComponent implements ExportRegistryProvisoning{
	
	private final Map<Object, ServiceRegistration> registrations;
	private final BundleContext context;
	
	public ExportRegistryComponent(BundleContext pContext) {
		context=pContext;
		registrations = new HashMap<Object, ServiceRegistration>();
	}
	
	@Validate
	@SuppressWarnings("unused")
	private void stop(){
		synchronized (registrations) {
			for (Iterator<ServiceRegistration> iterator = registrations.values().iterator(); iterator.hasNext();) {
				ServiceRegistration regis = (ServiceRegistration) iterator.next();
				regis.unregister();
				iterator.remove();
			}
		}
	}
	

	public void put(Object key, ExportReference xref) {
		
		synchronized (registrations) {
			
			if (registrations.containsKey(key)){
				throw new IllegalStateException("An EndpointDescription associated with the given key as already been registered");
			}
			
			ServiceRegistration reg = context.registerService(ExportReference.class.getName(), xref, endDescToDico(xref.getExportedEndpoint()));
			registrations.put(key, reg);
		}
		
	}

	public ExportReference remove(Object key) {
		ServiceRegistration sreg;

		synchronized (registrations) {
			sreg = registrations.remove(key);
		}
		
		if(sreg == null){
			return null;
		}

		ExportReference xref = (ExportReference) context.getService(sreg.getReference());
		sreg.unregister();

		return xref;
	}
	
	public boolean contains(Object key) {
		synchronized (registrations) {
			return registrations.containsKey(key);
		}
	}

}
