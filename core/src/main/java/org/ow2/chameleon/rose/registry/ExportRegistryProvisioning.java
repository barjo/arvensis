package org.ow2.chameleon.rose.registry;

import org.osgi.service.remoteserviceadmin.ExportReference;


/**
 * This service allows to register a newly available endpoint within an RRegistry.
 * @author barjo
 */
public interface ExportRegistryProvisioning {
	
	void put(Object key, ExportReference xref);
	
	ExportReference remove(Object key);
	
	boolean contains(ExportReference xref);
}

