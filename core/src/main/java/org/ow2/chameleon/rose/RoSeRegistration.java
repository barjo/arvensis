package org.ow2.chameleon.rose;

import org.osgi.service.remoteserviceadmin.ExportReference;

public interface RoSeRegistration {

	ExportReference getReference();
	
	void unRegister();
}
