package org.ow2.chameleon.rose.configurator;

import org.ow2.chameleon.rose.OutConnection;

public class DExporterConfiguration implements RoseConfiguration {
	private final OutConnection dexp;
	public DExporterConfiguration(OutConnection dexporter) {
		dexp=dexporter;
	}

	public void start() {
		dexp.start();
	}

	public void stop() {
		dexp.stop();
	}
}
