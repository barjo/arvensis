package org.ow2.chameleon.rose.configurator;

import static org.osgi.service.log.LogService.LOG_INFO;
import static org.osgi.service.log.LogService.LOG_WARNING;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
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
import org.ow2.chameleon.rose.api.InConnection;
import org.ow2.chameleon.rose.api.Instance;
import org.ow2.chameleon.rose.api.Machine;
import org.ow2.chameleon.rose.api.OutConnection;

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

    private Machine getMachine(String machineId){
        Collection<Machine> machs = machines.values();
        for(Machine machine : machs){
            if (machine.getId().equals(machineId))
                return machine;
        }

        return null;
    }

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
                for(Instance instance : machine.getInstances())
                    instance.update(existing);
                for(InConnection in : machine.getIns()){
                    in.update(existing);
                }
                for(OutConnection out : machine.getOuts()){
                    out.update(existing);
                }
                machines.put(name,existing);
            }

			logger.log(LOG_INFO, "Configuration "+name+" successfully handled");
		}
		catch(Exception e){
			logger.log(LOG_WARNING, "Cannot parse "+name+" an exception occured",e);
			throw e;
		}
	}


	public void uninstall(File file) throws Exception {
		String name = file.getName();

        Map<String, Object> json;

        try{
            json = jsonservice.fromJSON(new FileInputStream(file));
        }catch(Exception e){
            logger.log(LOG_WARNING, "Cannot parse removed rose configuration file: "+name,e);
            throw e;
        }

        try{
            Machine machine = parser.parse(json);

            if (machines.containsKey(name)){
                Machine existing = machines.remove(name);

                if(!containsMachine(existing.getId())){
                    existing.stop();
                } else{

                    for(Instance instance : machine.getInstances())
                        existing.remove(instance);

                    for(OutConnection out : machine.getOuts())
                        existing.remove(out);

                    for(InConnection in : machine.getIns()){
                        existing.remove(in);
                    }
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
		
		if (machines.containsKey(name)) {
		    logger.log(LOG_WARNING, "Updating a configuration file is not supported! please delete and create a new one.");
		}else {
			install(file); //handle as new
		}

	}
}

