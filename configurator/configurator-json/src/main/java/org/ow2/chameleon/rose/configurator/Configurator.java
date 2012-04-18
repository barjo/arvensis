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
import org.ow2.chameleon.rose.api.Machine;

/**
 * 
 */
@Component(name="RoSe_configurator.json")
@Instantiate(name="RoSe_configurator.json[0]")
@Provides
public class Configurator implements ArtifactInstaller{
	private static final String THIS_COMPONENT="RoSe.configurator";

	private static final String ROSE_CONF_REGX = "^rose-conf(-[a-zA-Z_0-9]+|).json$";
	
	private Map<String,Machine> machines = new HashMap<String, Machine>();
	
	
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
		parser = new ConfigurationParser(context,logger);
	}
	
	@Validate
	private void start(){
		logger.log(LogService.LOG_INFO, THIS_COMPONENT+" is starting");
	}

	@Invalidate
	private void stop(){
		for (Machine machine : machines.values()) {
			machine.stop();
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

	@SuppressWarnings("unchecked")
	public void install(File file) throws Exception {
		String name = file.getName();

		//GUARD - If the configuration already exists update
		if (machines.containsKey(name)){
			update(file);
			return;
		}
		
		logger.log(LOG_INFO, "Start to load configuration file: "+name);
		
		Map<String, Object> json;
		
		try{
			json = jsonservice.fromJSON(new FileInputStream(file));
		}catch(Exception e){
			logger.log(LOG_WARNING, "Cannot parse rose configuration file: "+name,e);
			throw e;
		}
		
		
		try{
			Machine machine = parser.parse(json);
			machine.start();
			machines.put(name, machine);
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
		
		if (machines.containsKey(name)){
			machines.remove(name).stop();
			logger.log(LOG_INFO, "The file: "+name+" as been removed. The corresonding Rose configuration has been destroyed.");
		}
		
	}
	

	@SuppressWarnings("unchecked")
	public void update(File file) throws Exception {
		String name = file.getName();
		logger.log(LOG_INFO, "Start to reload configuration file: "+name);
		
		if (machines.containsKey(name)) {
			Map<String, Object> json;

			try {
				json = jsonservice.fromJSON(new FileInputStream(file));
			} catch (Exception e) {
				logger.log(LOG_WARNING, "Cannot parse updated rose configuration file: " + name, e);
				throw e;
			}

			try { //stop and remove oldconf, then start newconf
				Machine machine = parser.parse(json);
				machines.remove(name).stop();
				machine.start();
				machines.put(name, machine);
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

