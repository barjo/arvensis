package org.ow2.chameleon.rose;

import java.util.Map;

import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.registry.ExportRegistry;
import org.ow2.chameleon.rose.registry.ImportRegistry;

public interface RoseMachine {

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
