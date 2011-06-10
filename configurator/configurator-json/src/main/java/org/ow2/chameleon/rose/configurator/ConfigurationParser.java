package org.ow2.chameleon.rose.configurator;

import static org.osgi.service.remoteserviceadmin.RemoteConstants.ENDPOINT_FRAMEWORK_UUID;
import static org.ow2.chameleon.rose.RoseMachine.ROSE_MACHINE_HOST;
import static org.ow2.chameleon.rose.RoseMachine.ROSE_MACHINE_ID;

import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.rose.DynamicExporter;
import org.ow2.chameleon.rose.DynamicImporter;

public class ConfigurationParser {
	
	private static final String MACHINE_ID = "id";
	private static final String MACHINE_HOST = "host";
	private static final String COMPONENT = "component";
	private static final String PROPERTIES = "properties";
	private static final String MACHINE_COMPONENT = "RoSe_machine";
	private static final String IN = "in";
	private static final String OUT = "out";
	private static final String ENDPOINT_FILTER = "endpoint.filter";
	private static final String SERVICE_FILTER = "service.filter";
	private static final String IMPORTER_FILTER = "importer.filter";
	private static final String EXPORTER_FILTER = "exporter.filter";
	
	private final BundleContext context;
	
	public ConfigurationParser(BundleContext pContext) {
		context = pContext;
	}
	
	public enum ConfType {
		machine,
		importer,
		exporter,
		registry,
		connection
	}
	
	public void parse(ConfType type, Map json, String machineId,GlobalRoseConfiguration conf) throws InvalidSyntaxException {
		
		switch (type) {
			case machine:
				parseMachine(json,conf);
				break;
			case connection:
				parseConnection(json,machineId,conf);
				break;
			case exporter:
			case importer:
			case registry:
				parseComponent(json,machineId,conf);
		}
		
	}
	

	private void parseMachine(Map<String, Object> json, GlobalRoseConfiguration conf) throws InvalidSyntaxException {
		
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		String id = null;
		
		//Get & Set id
		if (json.containsKey(MACHINE_ID)){
			id = (String) json.remove(MACHINE_ID);
		}
		
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
		properties.put("instance.name", MACHINE_COMPONENT+"-"+id);
		
		//Get & Set host
		if (json.containsKey(MACHINE_HOST)){
			properties.put(ROSE_MACHINE_HOST, (String) json.remove(MACHINE_HOST));
		}
		
		//Ok Add the machine
		conf.add(new FactoryTrackerConfiguration(context,MACHINE_COMPONENT,properties,null));
		
		//Check other conf
		for (String key : json.keySet()) {
			ConfType type = ConfType.valueOf(key);
			parse(type, (Map) json.get(key), id,conf);
		}
		
	}

	
	private void parseComponent(Map<String,Object> json, String machineId,GlobalRoseConfiguration conf) throws InvalidSyntaxException {
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		
		//mandatory
		String component = (String) json.get(COMPONENT);
		
		//Optional
		if (json.containsKey(PROPERTIES)){
			properties.putAll((Map<String,Object>) json.get(PROPERTIES));
		}
		
		conf.add(new FactoryTrackerConfiguration(context,component,properties,machineId));
	}


	private void parseConnection(Map json, String machineId,GlobalRoseConfiguration conf) throws InvalidSyntaxException {
		
		if (json.containsKey(IN)){
			Map<String,Object> inmap = (Map<String, Object>) json.get(IN);
			
			//Mandatory
			String endpoint = (String) inmap.get(ENDPOINT_FILTER);
			DynamicImporter.Builder builder = new DynamicImporter.Builder(context, endpoint);
			
			//Optional IMPORTER_FILTER
			if (inmap.containsKey(IMPORTER_FILTER)){
				builder.importerFilter((String) inmap.get(IMPORTER_FILTER));
			}
			
			//optional PROPERTIES
			if(inmap.containsKey(PROPERTIES)){
				builder.extraProperties((Map<String, Object>) inmap.get(PROPERTIES));
			}
				
			conf.add(new DImporterConfiguration(builder.build()));
		}
		
		
		
		
		if (json.containsKey(OUT)){
			Map<String,Object> outmap = (Map<String, Object>) json.get(OUT);
			
			//Mandatory
			String service = (String) outmap.get(SERVICE_FILTER);
			DynamicExporter.Builder builder = new DynamicExporter.Builder(context, service);
			
			//Optional EXPORTER_FILTER
			if (outmap.containsKey(EXPORTER_FILTER)){
				builder.exporterFilter((String) outmap.get(EXPORTER_FILTER));
			}
			
			//optional PROPERTIES
			if(outmap.containsKey(PROPERTIES)){
				builder.extraProperties((Map<String, Object>) outmap.get(PROPERTIES));
			}
				
			conf.add(new DExporterConfiguration(builder.build()));
		}
	}


	public RoseConfiguration parse(Map<String, Map> json) throws InvalidSyntaxException {
		GlobalRoseConfiguration conf = new GlobalRoseConfiguration();
		
		//Parse each entry
		for (String key : json.keySet()) {
			ConfType type = ConfType.valueOf(key);
			parse(type, (Map) json.get(key), null,conf);
		}
		
		return conf;
	}
	
}
