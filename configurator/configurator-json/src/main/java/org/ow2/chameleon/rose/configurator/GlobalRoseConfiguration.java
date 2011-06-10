package org.ow2.chameleon.rose.configurator;

import java.util.ArrayList;
import java.util.List;

public class GlobalRoseConfiguration implements RoseConfiguration {
	
	private List<RoseConfiguration> confs = new ArrayList<RoseConfiguration>();

	public synchronized void start() {
		for (RoseConfiguration conf : confs) {
			conf.start();
		}
	}

	public synchronized void stop() {
		for (RoseConfiguration conf : confs) {
			conf.stop();
		}
	}

	public synchronized void add(RoseConfiguration conf) {
		confs.add(conf);
	}
}
