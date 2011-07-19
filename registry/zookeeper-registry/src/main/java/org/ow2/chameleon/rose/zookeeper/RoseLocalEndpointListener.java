package org.ow2.chameleon.rose.zookeeper;

import static org.apache.zookeeper.CreateMode.EPHEMERAL;
import static org.osgi.service.log.LogService.LOG_DEBUG;
import static org.osgi.service.log.LogService.LOG_INFO;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.ow2.chameleon.rose.RoseMachine.ENDPOINT_LISTENER_INTEREST;
import static org.ow2.chameleon.rose.zookeeper.ZookeeperManager.computePath;
import static org.ow2.chameleon.rose.RoseMachine.EndpointListerInterrest.LOCAL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.ow2.chameleon.json.JSONService;

public class RoseLocalEndpointListener implements EndpointListener {
	
	private final ZookeeperManager bridge;
	private final ServiceRegistration registration;
	
	public RoseLocalEndpointListener(ZookeeperManager zbridge,String filter,BundleContext context) {
		bridge = zbridge;
		
		Dictionary<String, Object> props = null;
		props=new Hashtable<String, Object>();	
		props.put(ENDPOINT_LISTENER_INTEREST, LOCAL); //track only local EndpointDescription

		if (filter!=null){
			props.put(ENDPOINT_LISTENER_SCOPE, filter); //scope filter

		}

		registration = context.registerService(EndpointListener.class.getName(), this, props);
		logger().log(LOG_INFO, "The zookeeper ExportedEndpointListener service has been registered.");
	}
	
	/*----------------------------*
	 * EndpointListener call-back *
	 * (WB pattern)               *
	 *----------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.osgi.service.remoteserviceadmin.EndpointListener#endpointAdded(org.osgi.service.remoteserviceadmin.EndpointDescription, java.lang.String)
	 */
	public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
		//Convert the endpoint into a json object
		String desc = json().toJSON(endpoint.getProperties());
		try {
			String path = keeper().create(computePath(endpoint), desc.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, EPHEMERAL);
			logger().log(LOG_DEBUG,"The endpoint :"+endpoint.getId()+" has been published on the zookeeper node of path "+path);
		} catch (KeeperException e) {
			logger().log(LOG_WARNING, "Cannot publish through zookeeper the endpoint of id: "+endpoint.getId(),e);
		} catch (InterruptedException e) {
			logger().log(LOG_WARNING, "Cannot publish through zookeeper the endpoint of id: "+endpoint.getId(),e);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.service.remoteserviceadmin.EndpointListener#endpointRemoved(org.osgi.service.remoteserviceadmin.EndpointDescription, java.lang.String)
	 */
	public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
		final String path = computePath(endpoint);
		try {
			keeper().delete(path, -1); //-1 match any nodes version
			logger().log(LOG_DEBUG,"The endpoint :"+endpoint.getId()+" has been removed from zookeeper, node :"+path);
		} catch (InterruptedException e) {
			logger().log(LOG_WARNING, "Cannot remove from zookeeper the endpoint of id: "+endpoint.getId(),e);
		} catch (KeeperException e) {
			logger().log(LOG_WARNING, "Cannot remove from zookeeper the endpoint of id: "+endpoint.getId(),e);
		}
		
	}
	
	/**
	 * Unregister this {@link LocalEndpointListener}.
	 */
	public void destroy(){
		registration.unregister();
		logger().log(LOG_INFO, "The zookeeper ExportedEndpointListener service has been unregistered.");
	}
	
	/**
	 * @return The {@link LogService}
	 */
	private LogService logger(){
		return bridge.getLogger();
	}
	
	/**
	 * @return The {@link ZooKeeper} client
	 */
	private ZooKeeper keeper(){
		return bridge.getKeeper();
	}
	
	/**
	 * @return The {@link JSONService}
	 */
	private JSONService json(){
		return bridge.getJson();
	}
}
