package org.ow2.chameleon.rose.registry;

import org.osgi.service.remoteserviceadmin.ExportReference;

/**
 * 
 * @author barjo
 */
public interface RRegistration {

	/**
	 * 
	 * @return
	 */
	ExportReference getReference();
	
	/**
	 * 
	 */
	void unRegister();
}
