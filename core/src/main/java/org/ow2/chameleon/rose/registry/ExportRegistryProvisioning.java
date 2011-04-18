package org.ow2.chameleon.rose.registry;

import org.osgi.service.remoteserviceadmin.ExportReference;


/**
 * This service allows to register a newly available endpoint within an RRegistry.
 * @author barjo
 */
public interface ExportRegistryProvisioning {
	
	void put(ExportReference xref);

	boolean remove(ExportReference key);

	boolean contains(ExportReference key);
}

