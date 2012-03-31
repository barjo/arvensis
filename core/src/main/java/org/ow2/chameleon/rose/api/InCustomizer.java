package org.ow2.chameleon.rose.api;

import java.util.Map;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.ow2.chameleon.rose.ImporterService;

public interface InCustomizer {
	
	/**
	 * @return
	 */
	public ImportReference[] getImportReferences() throws UnsupportedOperationException;
	
	public Object doImport(ImporterService importer, EndpointDescription description,Map<String,Object> properties);
	
	public void unImport(ImporterService importer,EndpointDescription description, Object registration);
	
}
