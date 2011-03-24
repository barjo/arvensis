package org.ow2.chameleon.rose.supervisor;

import org.ow2.chameleon.rose.ExporterService;
import org.osgi.service.log.LogService;

/**
 * TODO complete
 */
public class ExportSupervisor {

	private LogService logger; //The log service
	private ExporterService exporter; //The exporter service

	private String filter; //filter use to track the service to be exported


	private void start(){
	}

	private void stop(){
	}


	private void bindExporterService(ExporterService pExporter){
		exporter = pExporter;
	}
}

