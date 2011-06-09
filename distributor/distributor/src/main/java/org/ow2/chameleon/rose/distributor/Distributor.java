package org.ow2.chameleon.rose.distributor;

import static org.osgi.service.log.LogService.LOG_INFO;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.ow2.chameleon.rose.distributor.ConfigurationParser.parseClientConf;
import static org.ow2.chameleon.rose.distributor.ConfigurationParser.parseServerConf;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.DynamicExporter;
import org.ow2.chameleon.rose.DynamicImporter;

/**
 * 
 */
@Component(name="RoSe.distributor")
@Instantiate
@Provides
public class Distributor implements ArtifactInstaller{

	private static final String ROSE_FILE_REGX = "^rose-config(-[a-zA-Z_0-9]?).json$";

	private static final String CLIENT_KEY = "client";

	private static final String SERVER_KEY = "server";
	
	private Map<String,DynamicImporter> dynimps = new HashMap<String, DynamicImporter>();
	
	private Map<String,DynamicExporter> dynexps = new HashMap<String, DynamicExporter>();
	
	@Requires(optional=true)
	private LogService logger; //The log service
	private BundleContext context; //BundleContext, set in the constructor
	
	@Requires(optional=false)
	private JSONService jsonservice; //JsonService, in order to parse the file
	
	/**
	 * Constructor, the {@link BundleContext} is injected by iPOJO.
	 * @param pContext
	 */
	public Distributor(BundleContext pContext) {
		context=pContext;
	}
	
	@Validate
	@SuppressWarnings("unused")
	private void start(){
		logger.log(LogService.LOG_INFO, "RoSe.distributor is starting");
	}

	@Invalidate
	@SuppressWarnings("unused")
	private void stop(){
		logger.log(LogService.LOG_INFO, "RoSe.distributor is stopping");
	}

	
	/*-----------------*
	 *  File handling  *
	 *-----------------*/
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
	 */
	public boolean canHandle(File file) {
		return file.getName().matches(ROSE_FILE_REGX);
	}

	public void install(File file) throws Exception {
		String name = file.getName();
		
		try{
			Map<String, Map> json = jsonservice.fromJSON(new FileInputStream(file));
		
			if (json.containsKey(CLIENT_KEY)){
				DynamicImporter dynimp = parseClientConf(context, json.get(CLIENT_KEY));
				dynimps.put(name, dynimp);
				dynimp.start();
				logger.log(LOG_INFO, "The file: "+name+" as been correctly handled by the distributor. A DynamicImporter has been started.");
			}
		
			if (json.containsKey(SERVER_KEY)){
				DynamicExporter dynexp = parseServerConf(context, json.get(SERVER_KEY));
				dynexps.put(name, dynexp);
				dynexp.start();
				logger.log(LOG_INFO, "The file: "+name+" as been correctly handled by the distributor. A DynamicExporter has been started.");
			}
		}
		catch(Exception e){
			logger.log(LOG_WARNING, "Cannot parse "+name+" an exception occured",e);
		}
	}


	public void uninstall(File file) throws Exception {
		String name = file.getName();
		
		if (dynimps.containsKey(name)){
			dynimps.remove(name).stop();
			logger.log(LOG_INFO, "The file: "+name+" as been removed. The corresonding DynamicImporter has been destroyed.");
		}
		
		if (dynexps.containsKey(name)){
			dynexps.remove(name).stop();
			logger.log(LOG_INFO, "The file: "+name+" as been removed. The corresonding DynamicExporter has been destroyed.");
		}
		
	}
	

	public void update(File file) throws Exception {
		String name = file.getName();
		try {
			Map json = (Map) jsonservice.fromJSON(new FileInputStream(file));

			if (dynimps.containsKey(name)) {
				dynimps.remove(name).stop();
				DynamicImporter dynimp = parseClientConf(context,
						(Map) json.get(CLIENT_KEY));

				if (dynimp != null) {
					dynimps.put(name, dynimp);
					dynimp.start();
				}
			}

			else if (dynexps.containsKey(name)) {
				dynexps.remove(name).stop();
				DynamicExporter dynexp = parseServerConf(context,
						(Map) json.get(SERVER_KEY));

				if (dynexp != null) {
					dynexps.put(name, dynexp);
					dynexp.start();
				}
			}
		} catch (Exception e) {
			logger.log(LOG_WARNING, "Cannot parse " + name
					+ " an exception occured", e);
		}
	}
}

