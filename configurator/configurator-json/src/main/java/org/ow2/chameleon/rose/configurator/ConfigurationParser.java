package org.ow2.chameleon.rose.configurator;

import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.ow2.chameleon.rose.RoseMachine.ROSE_MACHINE_HOST;
import static org.ow2.chameleon.rose.RoseMachine.ROSE_MACHINE_ID;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ComponentToken.factory;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfToken.component;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfToken.connection;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConfToken.machine;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConnectionToken.in;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.ConnectionToken.out;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.InToken.endpoint_filter;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.InToken.importer_filter;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.MachineToken.host;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.OutToken.exporter_filter;
import static org.ow2.chameleon.rose.configurator.ConfigurationParser.OutToken.service_filter;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.ipojo.parser.ParseException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.rose.DynamicExporter;
import org.ow2.chameleon.rose.DynamicImporter;

/**
 * Create a RoseConfiguration object for a given Map based configuration. 
 **/
public class ConfigurationParser {
	public static int confnum = 0;
	
	private static final String MACHINE_COMPONENT = "RoSe_machine";
	
	private final BundleContext context;
	
	public ConfigurationParser(BundleContext pContext) {
		context = pContext;
	}
	
	public enum ConfToken {
		machine,
		component,
		connection;
		
		public Object getValue(Map<String,Object> values){
			return values.get(this.toString());
		}
		
		public boolean isIn(Map<String, Object> json) {
			return json.containsKey(this.toString());
		}
	}
	
	public enum MachineToken{
		id,
		host;
		
		public String getValue(Map<String,Object> values){
			return (String) values.remove(this.toString());
		}

		public boolean isIn(Map<String, Object> json) {
			return json.containsKey(this.toString());
		}
	}
	
	public enum ComponentToken{
		factory,
		properties;
		
		public Object getValue(Map<String,Object> values){
			return values.remove(this.toString());
		}
		
		public boolean isIn(Map<String, Object> json) {
			return json.containsKey(this.toString());
		}
	}
	
	public enum ConnectionToken{
		in,
		out;
		
		public boolean isIn(Map<String, Object> json) {
			return json.containsKey(this.toString());
		}
		
		public Map<String, Object> getValue(Map<String,Object> values){
			return (Map<String, Object>) values.remove(this.toString());
		}
	}
	
	public enum InToken {
		endpoint_filter,
		importer_filter,
		properties;
		
		public Object getValue(Map<String,Object> values){
			return values.remove(this.toString());
		}
		
		public boolean isIn(Map<String, Object> json) {
			return json.containsKey(this.toString());
		}
	}
	
	public enum OutToken {
		service_filter,
		exporter_filter,
		properties;
		
		public Object getValue(Map<String,Object> values){
			return values.remove(this.toString());
		}
		
		public boolean isIn(Map<String, Object> json) {
			return json.containsKey(this.toString());
		}
	}

	private void parseMachine(Object obj, GlobalRoseConfiguration conf) throws InvalidSyntaxException, ParseException {
		
		if ( !(obj instanceof Map)){
			throw new ParseException(machine+" must contains a valid machine description: "+obj+" is not a valid jsonobject");
		}
		
		Map<String,Object> json = (Map<String,Object>) obj;
		
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		String id = MachineToken.id.getValue(json);
		
		//Get & Set id
		if (id == null){
			id = context.getProperty(ROSE_MACHINE_ID);
		}
		
		if (id == null){
			id = context.getProperty(ENDPOINT_FRAMEWORK_UUID);
		}
		
		if (id == null){
			id = UUID.randomUUID().toString();
		}
		
		properties.put(ROSE_MACHINE_ID, id);
		properties.put("instance.name", MACHINE_COMPONENT+"-"+confnum+"_"+id);
		
		//Get & Set host
		if (host.isIn(json)){
			properties.put(ROSE_MACHINE_HOST, host.getValue(json));
		}
		
		//Ok Add the machine
		conf.add(new FactoryTrackerConfiguration(context,MACHINE_COMPONENT,properties,null));
		
		//Check other conf
		conf.add(parse(json, id));
		
	}

	
	private void parseComponent(Object obj, String machineId,GlobalRoseConfiguration conf) throws InvalidSyntaxException, ParseException {
		if ( !(obj instanceof List)){
			throw new ParseException(component+" must contains a valid component description: "+obj+" is not a valid jsonobject");
		}
		
		List<Map> jsons = (List) obj;
		
		for (Map json : jsons) {
			Hashtable<String, Object> properties = new Hashtable<String, Object>();
		
			//mandatory
			String component = (String) factory.getValue(json);
			
			//Set a machine related instance name if in a machine
			if (machineId != null){
				properties.put("instance.name", component+"-"+confnum+"_"+machineId);
			}
		
			//Optional
			if (ComponentToken.properties.isIn(json)){
				properties.putAll((Map) ComponentToken.properties.getValue(json));
			}
		
			conf.add(new FactoryTrackerConfiguration(context,component,properties,machineId));
		}
	}

	private void parseConnection(Object obj, String machineId,GlobalRoseConfiguration conf) throws InvalidSyntaxException, ParseException {
		if ( !(obj instanceof List)){
			throw new ParseException(connection+" must contains a valid connection description: "+obj+" is not a valid jsonarray ");
		}
		
		List<Map> jsons = (List) obj;
		for (Map json : jsons) {
		
			if (in.isIn(json)){
				Map<String,Object> inmap = in.getValue(json);
			
				//Mandatory
				String endpoint = (String) endpoint_filter.getValue(inmap);
				DynamicImporter.Builder builder = new DynamicImporter.Builder(context, endpoint);
			
				//Optional IMPORTER_FILTER
				if (importer_filter.isIn(inmap)){
				builder.importerFilter((String) importer_filter.getValue(inmap));
				}
			
				//optional PROPERTIES
				if(InToken.properties.isIn(inmap)){
					builder.extraProperties((Map) InToken.properties.getValue(inmap));
				}
				
				conf.add(new DImporterConfiguration(builder.build()));
			}
		
			if (out.isIn(json)){
				Map<String,Object> outmap = out.getValue(json);
			
				//Mandatory
				String service = (String) service_filter.getValue(outmap);
				DynamicExporter.Builder builder = new DynamicExporter.Builder(context, service);
			
				//Optional EXPORTER_FILTER
				if (exporter_filter.isIn(outmap)){
					builder.exporterFilter((String) exporter_filter.getValue(outmap));
				}
			
				//optional PROPERTIES
				if(OutToken.properties.isIn(outmap)){
					builder.extraProperties((Map) OutToken.properties.getValue(outmap));
				}
				
				conf.add(new DExporterConfiguration(builder.build()));
			}
		}
	}


	public RoseConfiguration parse(Map<String, Object> json,String machineId) throws InvalidSyntaxException, ParseException {
		GlobalRoseConfiguration conf = new GlobalRoseConfiguration();
		
		if (machineId == null){ //incremente conf number
			confnum++;
		}
		
		//Parse each entry
		for (String key : json.keySet()) {
			ConfToken type = ConfToken.valueOf(key);
			Object obj = json.get(key);
			
			switch (type) {
				
			case machine:
				parseMachine(obj,conf);
				break;
			case connection:
				parseConnection(obj,machineId,conf);
				break;
			case component:
				parseComponent(obj,machineId,conf);
		  }
		}
		
		
		return conf;
	}
	
}
