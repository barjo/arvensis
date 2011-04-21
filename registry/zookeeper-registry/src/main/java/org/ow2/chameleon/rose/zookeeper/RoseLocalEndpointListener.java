package org.ow2.chameleon.rose.zookeeper;

import static org.apache.zookeeper.CreateMode.EPHEMERAL;
import static org.ow2.chameleon.rose.zookeeper.ZookeeperManager.computePath;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.registry.ExportedEndpointListener;

public class RoseLocalEndpointListener implements ExportedEndpointListener {
	
	private final ZookeeperManager bridge;
	private final ServiceRegistration registration;
	
	public RoseLocalEndpointListener(ZookeeperManager zbridge,String filter,BundleContext context) {
		bridge = zbridge;
		
		Dictionary<String, Object> props = null;
		
		if (filter!=null){
			props=new Hashtable<String, Object>();
			props.put(ENDPOINT_LISTENER_SCOPE, filter);
		}
		
		registration = context.registerService(ExportedEndpointListener.class.getName(), this, props);
		System.out.println("Register called: " + context.getBundle().getBundleId());
	}
	
	

	/*----------------------------*
	 * EndpointListener call-back *
	 * (WB pattern)               *
	 *----------------------------*/
	

	public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
		String desc = json().toJSON(endpoint.getProperties());
		try {
			String path = keeper().create(computePath(endpoint), desc.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, EPHEMERAL);
			System.out.println("Node create: "+path);
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
	}

	public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
		try {
			keeper().delete(computePath(endpoint), -1); //-1 match any nodes version
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (KeeperException e) {
			e.printStackTrace();
		}
		
	}
	
	public void stop(){
		System.out.println("IUnregister");
		registration.unregister();
	}
	
	
	
	private ZooKeeper keeper(){
		return bridge.getKeeper();
	}
	
	private JSONService json(){
		return bridge.getJson();
	}
}
