package org.ow2.chameleon.rose.internal;

import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;

public class BadImportRegistration implements ImportRegistration {
	private Throwable exception;

	public BadImportRegistration(Throwable throwable) {
		exception = throwable;
	}

	public ImportReference getImportReference() {
		return null;
	}

	public void close() {
		if (exception != null){
			exception = null;
		}
	}

	public Throwable getException() {
		return exception;
	}

}
