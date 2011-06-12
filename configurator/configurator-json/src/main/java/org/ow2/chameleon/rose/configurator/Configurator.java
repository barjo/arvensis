package org.ow2.chameleon.rose.configurator;

import static org.osgi.service.log.LogService.LOG_INFO;
import static org.osgi.service.log.LogService.LOG_WARNING;

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

/**
 * 
 */
@Component(name="RoSe.configurator")
@Instantiate
@Provides
public class Configurator implements ArtifactInstaller{
	private static final String THIS_COMPONENT="RoSe.configurator";

	private static final String ROSE_CONF_REGX = "^rose-conf(-[a-zA-Z_0-9]|).json$";
	
	private Map<String,RoseConfiguration> confs = new HashMap<String, RoseConfiguration>();
	
	
	@Requires(optional=true)
	private LogService logger; //The log service
	private final BundleContext context; //BundleContext, set in the constructor
	private final ConfigurationParser parser;
	
	
	@Requires(optional=false)
	private JSONService jsonservice; //JsonService, in order to parse the file
	
	/**
	 * Constructor, the {@link BundleContext} is injected by iPOJO.
	 * @param pContext
	 */
	public Configurator(BundleContext pContext) {
		context=pContext;
		parser = new ConfigurationParser(context);
	}
	
	@Validate
	@SuppressWarnings("unused")
	private void start(){
		logger.log(LogService.LOG_INFO, THIS_COMPONENT+" is starting");
	}

	@Invalidate
	@SuppressWarnings("unused")
	private void stop(){
		for (RoseConfiguration conf : confs.values()) {
			conf.stop();
		}
		logger.log(LogService.LOG_INFO, THIS_COMPONENT+" is stopping");
	}

	
	/*-----------------*
	 *  File handling  *
	 *-----------------*/
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.felix.fileinstall.ArtifactListener#canHandle(java.io.File)
	 */
	public boolean canHandle(File file) {
		return file.getName().matches(ROSE_CONF_REGX);
	}

	public void install(File file) throws Exception {
		String name = file.getName();
		logger.log(LOG_INFO, "Start to load configuration file: "+name);
		
		Map<String, Object> json;
		
		try{
			json = jsonservice.fromJSON(new FileInputStream(file));
		}catch(Exception e){
			logger.log(LOG_WARNING, "Cannot parse rose configuration file: "+name,e);
			throw e;
		}
		
		
		try{
			RoseConfiguration conf = parser.parse(json,null);
			conf.start();
			confs.put(name, conf);
			logger.log(LOG_INFO, "Configuration "+name+" successfully handled");
		}
		catch(Exception e){
			logger.log(LOG_WARNING, "Cannot parse "+name+" an exception occured",e);
			throw e;
		}
	}


	public void uninstall(File file) throws Exception {
		String name = file.getName();
		logger.log(LOG_INFO, "Configuration file: "+name +" removed");
		
		if (confs.containsKey(name)){
			confs.remove(name).stop();
			logger.log(LOG_INFO, "The file: "+name+" as been removed. The corresonding Rose configuration has been destroyed.");
		}
		
	}
	

	public void update(File file) throws Exception {
		String name = file.getName();
		logger.log(LOG_INFO, "Start to reload configuration file: "+name);
		
		if (confs.containsKey(name)) {
			Map<String, Object> json;

			try {
				json = jsonservice.fromJSON(new FileInputStream(file));
			} catch (Exception e) {
				logger.log(LOG_WARNING, "Cannot parse updated rose configuration file: " + name, e);
				throw e;
			}

			try {
				RoseConfiguration newconf = parser.parse(json,null);
				newconf.start();
				confs.remove(name).stop();
				confs.put(name, newconf);
			} catch (Exception e) {
				logger.log(LOG_WARNING, "Cannot parse updated rose configuration file:" + name
						+ " an exception occured", e);
				throw e;
			}
		}else {
			install(file); //handle as new
		}

	}
}

