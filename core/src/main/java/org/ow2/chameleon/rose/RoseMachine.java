package org.ow2.chameleon.rose;

import java.util.Map;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.registry.ExportRegistry;
import org.ow2.chameleon.rose.registry.ImportRegistry;

public interface RoseMachine {
	
	/**
	 * System property identifying the ID for this rose machine.
	 */
	public final static String ROSE_MACHINE_ID = "rose.machine.id";

	/**
	 * System property identifying the host name for this rose machine.
	 */
	public final static String ROSE_MACHINE_HOST = "rose.machine.host";

	/**
	 * System property identifying the IP address for this rose machine.
	 */
	public final static String ROSE_MACHINE_IP = "rose.machine.ip";

	public final static String ENDPOINT_LISTENER_INTEREST = "endpoint.listener.interrest";

	public enum EndpointListerInterrest {
		LOCAL,REMOTE,ALL;
	}

	/**
	 * @return This RoSe machine local registry which contains all discovered
	 *         {@link EndpointDescription} related to remote services.
	 */
	public ImportRegistry importRegistry();

	/**
	 * @return This RoSe machine local registry which contains all published
	 *         {@link EndpointDescription} related to local service.
	 */
	public ExportRegistry exportRegistry();
	
	
	/**
	 * @return This rose machine id.
	 */
	public String getId();

	/**
	 * @return This rose machine host.
	 */
	public String getHost();

	/**
	 * @return This rose machine ip.
	 */
	public String getIP();

	/**
	 * @return This RoSe machine properties.
	 */
	public Map<String, Object> getProperties();
}
