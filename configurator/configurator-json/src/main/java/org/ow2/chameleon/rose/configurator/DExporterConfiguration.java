package org.ow2.chameleon.rose.configurator;

import org.ow2.chameleon.rose.DynamicExporter;

public class DExporterConfiguration implements RoseConfiguration {
	private final DynamicExporter dexp;
	public DExporterConfiguration(DynamicExporter dexporter) {
		dexp=dexporter;
	}

	public void start() {
		dexp.start();
	}

	public void stop() {
		dexp.stop();
	}
}
