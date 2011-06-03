package org.ow2.chameleon.rose;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.remoteserviceadmin.ExportReference;

public interface DynamicExporterCustomizer {
	
	/**
	 * @return
	 */
	public ExportReference[] getExportReferences() throws UnsupportedOperationException;
	
	public Object export(ExporterService exporter, ServiceReference sref, Map<String,Object> properties);
	
	public void unExport(ExporterService exporter,ServiceReference sref, Object registration);
	
}
