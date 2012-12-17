package org.ow2.chameleon.rose.api;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.ow2.chameleon.rose.ExporterService;

public interface OutCustomizer {
	
	/**
	 * @return All {@link ExportReference} of of services exported through this dynamic exporter.
	 */
	public ExportReference[] getExportReferences() throws UnsupportedOperationException;
	
	/**
	 * 
	 * @param exporter
	 * @param sref
	 * @param properties
	 * @return 
	 */
	public Object export(ExporterService exporter, ServiceReference sref, Map<String,Object> properties);
	
	public void unExport(ExporterService exporter,ServiceReference sref, Object registration);
	
}
