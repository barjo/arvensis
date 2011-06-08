package org.ow2.chameleon.rose.zookeeper;

import static org.ow2.chameleon.rose.zookeeper.ZookeeperManager.SEPARATOR;

import java.util.Map;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseMachine;

public class ZooRemoteEndpointWatcher implements Watcher{
	
	private final ZookeeperManager manager;
	private final RoseMachine machine;
	
	public ZooRemoteEndpointWatcher(ZookeeperManager pManager,RoseMachine pMachine) {
		manager=pManager;
		machine=pMachine;
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
		if (path.startsWith(SEPARATOR+manager.frameworkid)){
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
				machine.putRemote(path, endpoint);
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
			machine.removeRemote(path);
			break;
		case NodeChildrenChanged:
			//Remove the endpoint
			machine.removeRemote(path); //?
			break;
			
		case None:
		default:
			break;
		}
	}
	
	private ZooKeeper keeper(){
		return manager.getKeeper();
	}
	
	private JSONService json(){
		return manager.getJson();
	}
}
