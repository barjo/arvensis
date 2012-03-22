package org.ow2.chameleon.rose.configurator;

import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.ow2.chameleon.rose.InConnection.InBuilder.in;
import static org.ow2.chameleon.rose.OutConnection.OutBuilder.out;
import static org.ow2.chameleon.rose.RoseMachine.RoSe_MACHINE_ID;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.MACHINE;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.component;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.connection;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.exporter_filter;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.factory;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.host;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.importer_filter;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.in;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.out;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.properties;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.protocol;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfType.service_filter;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.ipojo.parser.ParseException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.rose.InConnection.InBuilder;
import org.ow2.chameleon.rose.Machine;
import org.ow2.chameleon.rose.Machine.MachineBuilder;
import org.ow2.chameleon.rose.OutConnection.OutBuilder;

/**
 * Create a RoseConfiguration object for a given Map based configuration. 
 **/
public class ConfigurationParser {	
	private static final String MACHINE_COMPONENT = "RoSe_machine";
	
	private final BundleContext context;
	
	public ConfigurationParser(BundleContext pContext) {
		context = pContext;
	}
	
	
	public enum ConfType{
		MACHINE,
		component,
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
			return (String) values.remove(this.toString());
		}

		public boolean isIn(Map<String, Object> json) {
			return json.containsKey(this.toString());
		}
	}

	private Machine parseMachine(Object obj) throws InvalidSyntaxException, ParseException {
		Machine rosemachine;
		if ( !(obj instanceof Map)){
			throw new ParseException(MACHINE+" must contains a valid machine description: "+obj+" is not a valid jsonobject");
		}
		
		Map<String,Object> json = (Map<String,Object>) obj;
		
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
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
		
		properties.put(RoSe_MACHINE_ID, id);
		properties.put("instance.name", MACHINE_COMPONENT+"_"+id);
		
		//Get & Set host
		if (host.isIn(json)){
			mbuilder.host((String) host.getValue(json));
		}
		rosemachine = mbuilder.create();
		
		//Parse, the connections
		if (json.containsKey(connection)){
			parseConnection(connection.getValue(json), rosemachine);
		}
		
		//TODO parse components
		
		return rosemachine;
	}

	/**
	 * FIXME
	 */
	private void parseComponent(Object obj, Machine machine) throws InvalidSyntaxException, ParseException {
		if ( !(obj instanceof List)){
			throw new ParseException(component+" must contains a valid component description: "+obj+" is not a valid jsonobject");
		}
		
		List<Map> jsons = (List) obj;
		
		for (Map json : jsons) {
			Hashtable<String, Object> props = new Hashtable<String, Object>();
		
			//mandatory
			String component = (String) factory.getValue(json);
			
			//Set a machine related instance name if in a machine
			//props.put("instance.name", component+"_"+machineId);
		
			//Optional
			if (ConfType.properties.isIn(json)){
				props.putAll((Map) properties.getValue(json));
			}
		
			//conf.add(new FactoryTrackerConfiguration(context,component,props,machineId));
		}
	}

	private void parseConnection(Object obj, Machine machine) throws InvalidSyntaxException, ParseException {
		if ( !(obj instanceof List)){
			throw new ParseException(connection+" must contains a valid connection description: "+obj+" is not a valid jsonarray ");
		}
		
		List<Map> jsons = (List) obj;
		for (Map json : jsons) {
		
			if (in.isIn(json)){
				Map<String,Object> inmap = (Map<String, Object>) in.getValue(json);
			
				//Mandatory
				String endpoint = (String) ConfType.endpoint_filter.getValue(inmap);
				InBuilder inBuilder = in(machine, endpoint);
			
				//Optional protocols
				if (protocol.isIn(inmap)){
					inBuilder.protocol((List<String>) protocol.getValue(inmap));
				}
				
				//Optional IMPORTER_FILTER
				if (importer_filter.isIn(inmap)){
				inBuilder.importerFilter((String) importer_filter.getValue(inmap));
				}
			
				//optional PROPERTIES
				if(properties.isIn(inmap)){
					inBuilder.extraProperties((Map) properties.getValue(inmap));
				}
				
				inBuilder.create(); //create the connection
			}
		
			if (out.isIn(json)){
				Map<String,Object> outmap = (Map<String, Object>) out.getValue(json);
			
				//Mandatory
				String service = (String) service_filter.getValue(outmap);
				OutBuilder out = out(machine, service);
			
				if (protocol.isIn(outmap)){
					out.protocol((List<String>) protocol.getValue(outmap));
				}
				
				//Optional EXPORTER_FILTER
				if (exporter_filter.isIn(outmap)){
					out.exporterFilter((String) exporter_filter.getValue(outmap));
				}
			
				//optional PROPERTIES
				if(properties.isIn(outmap)){
					out.extraProperties((Map) properties.getValue(outmap));
				}
				
				//optional Customizer
				if (ConfType.customizer.isIn(outmap)){
					//TODO
				}
				
				out.create(); //Create the connection
			}
		}
	}


	public Machine parse(Map<String, Object> json) throws InvalidSyntaxException, ParseException {
		
		if (MACHINE.isIn(json)){
			return parseMachine(MACHINE.getValue(json));
		}
		else {
			throw new ParseException("The configuration does not contains a valid rose machine configuration");
		}
	}
	
}
