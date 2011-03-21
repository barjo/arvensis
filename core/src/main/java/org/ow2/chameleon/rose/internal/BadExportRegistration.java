package org.ow2.chameleon.rose.internal;

import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;

/**
 * Implementation of an {@link ExportRegistration} use to represent an aborted export.
 * 
 * @author barjo
 */
public final class BadExportRegistration implements ExportRegistration{
	public volatile Throwable exception;
	
	public BadExportRegistration(Throwable throwable) {
		exception = throwable;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.service.remoteserviceadmin.ExportRegistration#getExportReference()
	 */
	public ExportReference getExportReference() {
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.service.remoteserviceadmin.ExportRegistration#close()
	 */
	public void close() {
		if (exception !=null){
			exception = null;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.service.remoteserviceadmin.ExportRegistration#getException()
	 */
	public Throwable getException() {
		return exception;
	}
}