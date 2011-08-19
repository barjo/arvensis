package org.ow2.chameleon.rose;

import java.util.Map;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ExportReference;

public interface RoseMachine {
	
	/**
	 * System property identifying the ID for this rose machine.
	 */
	final static String ROSE_MACHINE_ID = "rose.machine.id";

	/**
	 * System property identifying the host name for this rose machine.
	 */
	final static String ROSE_MACHINE_HOST = "rose.machine.host";

	final static String ENDPOINT_LISTENER_INTEREST = "endpoint.listener.interrest";

	enum EndpointListerInterrest {
		LOCAL,REMOTE,ALL;
	}

	void putRemote(Object key, EndpointDescription description);

	EndpointDescription removeRemote(Object key);

	boolean containsRemote(EndpointDescription desc);

	
	void putLocal(Object key, ExportReference xref);

	ExportReference removeLocal(Object key);

	boolean containsLocal(ExportReference xref);
	
	void addEndpointListener(EndpointListener listener, EndpointListerInterrest interrest, String filter) throws InvalidSyntaxException;

	void removeEndpointListener(EndpointListener listener, EndpointListerInterrest interrest);
	
	
	/**
	 * @return This rose machine id.
	 */
	String getId();

	/**
	 * @return This rose machine host.
	 */
	String getHost();

	/**
	 * @return This RoSe machine properties.
	 */
	Map<String, Object> getProperties();
	
}
