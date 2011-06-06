package org.ow2.chameleon.rose;

import java.util.Map;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;

public interface DynamicImporterCustomizer {
	
	/**
	 * @return
	 */
	public ImportReference[] getImportReferences() throws UnsupportedOperationException;
	
	public Object doImport(ImporterService importer, EndpointDescription description,Map<String,Object> properties);
	
	public void unImport(ImporterService importer,EndpointDescription description, Object registration);
	
}
