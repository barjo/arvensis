package org.ow2.chameleon.rose.configurator;

import org.ow2.chameleon.rose.InConnection;

public class DImporterConfiguration implements RoseConfiguration {
	private final InConnection dimp;
	public DImporterConfiguration(InConnection dimporter) {
		dimp=dimporter;
	}

	public void start() {
		dimp.start();
	}

	public void stop() {
		dimp.stop();
	}
}
