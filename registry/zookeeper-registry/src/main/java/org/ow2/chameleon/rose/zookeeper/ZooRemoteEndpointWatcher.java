package org.ow2.chameleon.rose.zookeeper;

import static org.ow2.chameleon.rose.zookeeper.ZookeeperManager.SEPARATOR;

import java.util.Map;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.registry.ImportRegistryProvisioning;

public class ZooRemoteEndpointWatcher implements Watcher{
	
	private final ZookeeperManager bridge;
	private final ImportRegistryProvisioning registry;
	
	public ZooRemoteEndpointWatcher(ZookeeperManager zbridge,ImportRegistryProvisioning rregistry) {
		bridge=zbridge;
		registry=rregistry;
		try {
			keeper().exists(SEPARATOR, this);
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void stop(){
		
	}
	
	/*-----------------------------------*
	 *  Zookeeper Watcher method         *
	 *-----------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.apache.zookeeper.Watcher#process(org.apache.zookeeper.WatchedEvent)
	 */
	public void process(WatchedEvent event) {
		System.out.println("Provisionning event "+event);
		// TODO Auto-generated method stub
		String path = event.getPath();
		
		//Description published by this framework. do nothing
		if (path.startsWith(SEPARATOR+bridge.frameworkid)){
			return;
		}
		
		switch (event.getType()) {
		case NodeCreated:
			//Add the endpoint
			try {
				byte[] desc = keeper().getData(path, false, null);
				@SuppressWarnings("unchecked")
				Map<String, Object> map = json().fromJSON(String.valueOf(desc));
				EndpointDescription endpoint = new EndpointDescription(map);
				registry.put(path, endpoint);
				keeper().exists(path, this); //Watch the node
			} catch (Exception e) {
				// TODO: handle exception
			}
			
			break;
		case NodeDataChanged:
			break;
		case NodeDeleted:
			//Remove the endpoint
			System.out.println("Path "+path);
			registry.remove(path);
			break;
		case NodeChildrenChanged:
			//Remove the endpoint
			registry.remove(path); //?
			break;
			
		case None:
		default:
			break;
		}
	}
	
	private ZooKeeper keeper(){
		return bridge.getKeeper();
	}
	
	private JSONService json(){
		return bridge.getJson();
	}
}
