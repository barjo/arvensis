package org.ow2.chameleon.rose.configurator;

import org.ow2.chameleon.rose.DynamicImporter;

public class DImporterConfiguration implements RoseConfiguration {
	private final DynamicImporter dimp;
	public DImporterConfiguration(DynamicImporter dimporter) {
		dimp=dimporter;
	}

	public void start() {
		dimp.start();
	}

	public void stop() {
		dimp.stop();
	}
}
