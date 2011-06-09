package org.ow2.chameleon.rose.distributor;

import static java.lang.String.valueOf;
import static org.ow2.chameleon.rose.distributor.ConfigurationParser.Token.endpoint;
import static org.ow2.chameleon.rose.distributor.ConfigurationParser.Token.exporter;
import static org.ow2.chameleon.rose.distributor.ConfigurationParser.Token.importer;
import static org.ow2.chameleon.rose.distributor.ConfigurationParser.Token.properties;
import static org.ow2.chameleon.rose.distributor.ConfigurationParser.Token.service;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.rose.DynamicExporter;
import org.ow2.chameleon.rose.DynamicImporter;
import org.ow2.chameleon.rose.DynamicImporter.Builder;

public class ConfigurationParser {
	
	public static DynamicImporter parseClientConf(BundleContext context, Map conf) throws InvalidSyntaxException {
		Builder builder;
		
		if (!conf.containsKey(endpoint.toString())){
			throw new IllegalArgumentException("The key "+endpoint + " is mandatory");
		}
		
		//Mandatory ok create the builder
		
		builder = new DynamicImporter.Builder(context, valueOf(conf.get(endpoint.toString())));
		
		// Add the importer filter is specified
		if (conf.containsKey(importer)){
			builder.importerFilter(valueOf(conf.get(importer.toString())));
		}
		
		// Add the extra properties if specified 
		if (conf.containsKey(properties)){
			builder.extraProperties((Map) conf.get(properties.toString()));
		}

		return builder.build();
	}

	public static DynamicExporter parseServerConf(BundleContext context, Map conf) throws InvalidSyntaxException {
		DynamicExporter.Builder builder;
		
		if (!conf.containsKey(service.toString())){
			throw new IllegalArgumentException("The key "+service + " is mandatory");
		}
		
		//Mandatory ok create the builder
		
		builder = new DynamicExporter.Builder(context, valueOf(conf.get(service.toString())));
		
		// Add the importer filter is specified
		if (conf.containsKey(exporter.toString())){
			builder.exporterFilter(valueOf(conf.get(exporter.toString())));
		}
		
		// Add the extra properties if specified 
		if (conf.containsKey(properties.toString())){
			builder.extraProperties((Map) conf.get(properties.toString()));
		}

		return builder.build();
	}
	
	
	public static enum Token {
		exporter,
		importer,
		service,
		endpoint,
		properties
	}

}
