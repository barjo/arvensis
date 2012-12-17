package org.ow2.chameleon.rose.configurator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.log.LogService;
import org.ow2.chameleon.rose.api.InConnection.InBuilder;
import org.ow2.chameleon.rose.api.Instance.InstanceBuilder;
import org.ow2.chameleon.rose.api.Machine;
import org.ow2.chameleon.rose.api.Machine.MachineBuilder;
import org.ow2.chameleon.rose.api.OutConnection.OutBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.osgi.service.log.LogService.LOG_DEBUG;
import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.ow2.chameleon.rose.RoseMachine.RoSe_MACHINE_ID;
import static org.ow2.chameleon.rose.api.InConnection.InBuilder.in;
import static org.ow2.chameleon.rose.api.OutConnection.OutBuilder.out;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.*;

/**
 * Create a RoseConfiguration object for a given Map based configuration. 
 **/
public class ConfigurationParser {	
	private final BundleContext context;
	private final LogService logger;
	
	public ConfigurationParser(BundleContext pContext,LogService pLogger) {
		context = pContext;
		logger = pLogger;
	}
	
	
	public enum ConfType{
		machine,
		component,
        instance,
		connection,
		id,
		host,
		factory,
		properties,
		in,
		out,
		endpoint_filter,
		importer_filter,
		service_filter,
		exporter_filter,
		protocol,
		customizer;
		
		public Object getValue(Map<String,Object> values){
			return values.remove(this.toString());
		}

		public boolean isIn(Map<String, Object> json) {
			return json.containsKey(this.toString());
		}
	}

	private Machine parseMachine(Object obj) throws InvalidSyntaxException {
		Machine rosemachine;
		if ( !(obj instanceof Map)){
			throw new IllegalArgumentException(machine+" must contains a valid machine description: "+obj+" is not a valid jsonobject");
		}
		
		@SuppressWarnings("unchecked")
		Map<String,Object> json = (Map<String,Object>) obj;
		
		String id = (String) ConfType.id.getValue(json);
		
		//Get & Set id
		if (id == null){
			id = context.getProperty(RoSe_MACHINE_ID);
		}
		
		if (id == null){
			id = context.getProperty(ENDPOINT_FRAMEWORK_UUID);
		}
		
		if (id == null){
			id = UUID.randomUUID().toString();
		}
		
		MachineBuilder mbuilder = MachineBuilder.machine(context, id);
		
		//Get & Set host
		if (host.isIn(json)){
			mbuilder.host((String) host.getValue(json));
		}
		rosemachine = mbuilder.create();
		
		//Parse, the connections
		if (connection.isIn(json)){
			parseConnection(connection.getValue(json), rosemachine);
		}

        //Parse the instance/component conf
		if (component.isIn(json)){
			parseComponent(component.getValue(json), rosemachine);
		}

        if (instance.isIn(json)){
            parseComponent(instance.getValue(json),rosemachine);
        }
		
		return rosemachine;
	}

	/**
	 * FIXME
	 */
	@SuppressWarnings({ "unchecked" })
	private void parseComponent(Object list, Machine machine) throws InvalidSyntaxException {
		if ( !(list instanceof List)){
			throw new IllegalArgumentException(component+" must contains a valid component description: "+list+" is not a valid jsonobject");
		}
		
		logger.log(LOG_DEBUG, "Parse component instances of machine: " +machine.getId());

		
		InstanceBuilder ibuilder;
		
		List<Map<String,Object>> jsons = (List<Map<String, Object>>) list;
		
		for (Map<String, Object> json : jsons) {
			//mandatory
			String component = (String) factory.getValue(json);
			
			ibuilder = InstanceBuilder.instance(machine, component).name(component+"_"+machine.getId());
			
			//Optional
			if (ConfType.properties.isIn(json)){
				ibuilder.withProperties((Map<String, Object>)properties.getValue(json));
			}
		
			ibuilder.create();
		}
	}

	@SuppressWarnings("unchecked")
	private void parseConnection(Object list, Machine machine) throws InvalidSyntaxException {
		if ( !(list instanceof List)){
			throw new IllegalArgumentException(connection+" must contains a valid connection description: "+list+" is not a valid jsonarray ");
		}
		
		logger.log(LOG_DEBUG, "Parse connections of machine: " +machine.getId());
		
		List<Map<String,Object>> jsons = (List<Map<String, Object>>) list;
		for (Map<String, Object> json : jsons) {
		
			if (ConfType.in.isIn(json)){
				Map<String,Object> inmap = (Map<String, Object>) ConfType.in.getValue(json);
			
				//Mandatory
				String endpoint = (String) ConfType.endpoint_filter.getValue(inmap);
				InBuilder inBuilder = in(machine, endpoint);
			
				//Optional protocols
				if (protocol.isIn(inmap)){
					inBuilder.protocol((List<String>) protocol.getValue(inmap));
				}
				
				//Optional IMPORTER_FILTER
				if (importer_filter.isIn(inmap)){
				inBuilder.withImporter((String) importer_filter.getValue(inmap));
				}
			
				//optional PROPERTIES
				if(properties.isIn(inmap)){
					inBuilder.withProperties((Map<String,Object>) properties.getValue(inmap));
				}
				
				inBuilder.add(); //create the connection
			}
		
			if (ConfType.out.isIn(json)){
				Map<String,Object> outmap = (Map<String, Object>) ConfType.out.getValue(json);
			
				//Mandatory
				String service = (String) service_filter.getValue(outmap);
				OutBuilder out = out(machine, service);
			
				if (protocol.isIn(outmap)){
					out.protocol((List<String>) protocol.getValue(outmap));
				}
				
				//Optional EXPORTER_FILTER
				if (exporter_filter.isIn(outmap)){
					out.withExporter((String) exporter_filter.getValue(outmap));
				}
			
				//optional PROPERTIES
				if(properties.isIn(outmap)){
					out.withProperties((Map<String, Object>) properties.getValue(outmap));
				}
				
				//optional Customizer
				if (ConfType.customizer.isIn(outmap)){
					//TODO
				}
				
				out.add(); //Create the connection
			}
		}
	}


	public Machine parse(Map<String, Object> json) throws InvalidSyntaxException {
		
		if (machine.isIn(json)){
			return parseMachine(machine.getValue(json));
		}
		else {
			throw new IllegalArgumentException("The configuration does not contains a valid rose machine configuration");
		}
	}


}
