package org.ow2.chameleon.rose.configurator;

import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.api.Machine;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.osgi.service.log.LogService.LOG_INFO;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.ow2.chameleon.rose.util.RoseTools.removeFromMachine;
import static org.ow2.chameleon.rose.util.RoseTools.updateMachine;

/**
 * 
 */
@Component(name="RoSe_configurator.json")
@Instantiate(name="RoSe_configurator.json[0]")
@Provides
public class Configurator implements ArtifactInstaller{
	private static final String THIS_COMPONENT="RoSe.configurator";

	private static final String ROSE_CONF_REGX = "^rose-conf(-[a-zA-Z_0-9]+|).(json|rose)$";
	
	private Map<String,Machine> machines = new HashMap<String, Machine>();
    private Map<String, MachineConfiguration> confs = new HashMap<String, MachineConfiguration>();
	
	
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

    /**
     * @param machineId
     * @return The existing machine of given id.
     */
    private Machine getMachine(String machineId){
        Collection<Machine> machs = machines.values();
        for(Machine machine : machs){
            if (machine.getId().equals(machineId))
                return machine;
        }

        return null;
    }

    /**
     * @param machineId
     * @return <code>true</code> if a machine of given id exist, <code>false</code> otherwise.
     */
    private boolean containsMachine(String machineId){
        Collection<Machine> machs = machines.values();
        for(Machine machine : machs){
            if (machine.getId().equals(machineId))
                return true;
        }
        return false;
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
            Machine existing = getMachine(machine.getId());


            if (existing == null){
			    machine.start();
			    machines.put(name, machine);

            } else {
                updateMachine(existing,machine.getOuts(),machine.getIns(),machine.getInstances());
                machines.put(name,existing);
            }

            confs.put(name,new MachineConfiguration(machine)); //save the conf

			logger.log(LOG_INFO, "Configuration "+name+" successfully handled");
		}
		catch(Exception e){ //TODO fix error handling ?
			logger.log(LOG_WARNING, "Cannot parse "+name+" an exception occured",e);
			throw e;
		}
	}


	public void uninstall(File file) throws Exception {
		String name = file.getName();


        try{
            MachineConfiguration mconf = confs.remove(name);

            if (mconf != null){
                Machine existing = machines.remove(name);

                if(!containsMachine(existing.getId())){ //last configuration, stop the machine.
                    existing.stop();
                } else{ //just update the configuration
                    removeFromMachine(existing, mconf.getOuts(), mconf.getIns(), mconf.getInstances());
                }

                logger.log(LOG_INFO, "The file: "+name+" as been removed. The corresponding Rose configuration has been destroyed.");
            }


        } catch (Exception e){
            logger.log(LOG_WARNING, "Cannot parse removed rose configuration file: "+name,e);
        }
	}
	

	@SuppressWarnings("unchecked")
	public void update(File file) throws Exception {
		String name = file.getName();
		logger.log(LOG_INFO, "Start to reload configuration file: "+name);
		
		if (machines.containsKey(name)) {     //TODO something better ?
            uninstall(file);
            install(file);

		    logger.log(LOG_WARNING, "New configuration has been successfully handled");
		}else {
			install(file); //handle as new
		}

	}
}

