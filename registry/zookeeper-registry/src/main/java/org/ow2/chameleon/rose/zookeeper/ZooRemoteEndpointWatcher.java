package org.ow2.chameleon.rose.zookeeper;

import static org.ow2.chameleon.rose.zookeeper.ZookeeperManager.SEPARATOR;

import java.text.ParseException;
import java.util.List;
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
			System.out.println("Start watcher");
			List<String> nodes = keeper().getChildren(SEPARATOR, this);
			System.out.println("nodes: "+nodes);
			for (String gw : nodes) {
				if (gw.equals(manager.frameworkid)){
					continue; //ignore myself
				}
				processMachineAdded(SEPARATOR+gw);
			}
			
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ParseException e) {
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
				if (path.matches("^/([a-zA-Z_0-9]|-|_)+/([a-zA-Z_0-9]|-|_)+\\$")){
					System.out.println("register Endpoint");
					processEndpointAdded(event.getPath());
				} else if (path.matches("^/([a-zA-Z_0-9]|-|_)+\\$")) {
					System.out.println("add gateway");
					processMachineAdded(path);
				}
				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			
			break;
		case NodeDataChanged:
			break;
		case NodeDeleted:
			try {
				//Remove endpoint
				if (path.matches("^/([a-zA-Z_0-9]|-|_)+/([a-zA-Z_0-9]|-|_)+\\$")){
					System.out.println("Removed Endpoint");
					machine.removeRemote(path);
				} else if (path.matches("^/([a-zA-Z_0-9]|-|_)+$")) {
					System.out.println("Removed gateway");
					processMachineRemoved(path);
				}
			}catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			break;
		case NodeChildrenChanged:
			try {
				keeper().exists(path, this);
			} catch (Exception e) {
			}
			break;
			
		case None:
		default:
			break;
		}
	}
	
	private void processMachineAdded(String gwpth) throws KeeperException, InterruptedException, ParseException{
		List<String> endpoints = keeper().getChildren(gwpth,this);
		keeper().exists(gwpth, true);
		for (String endpoint : endpoints) {
			processEndpointAdded(gwpth+SEPARATOR+endpoint);
		}
	}
	
	private void processMachineRemoved(String gw) throws KeeperException, InterruptedException{
		List<String> endpoints = keeper().getChildren(gw,this);
		for (String endpoint : endpoints) {
			machine.removeRemote(gw+SEPARATOR+endpoint);
		}
	}
	
	private void processEndpointAdded(String endpath) throws KeeperException, InterruptedException, ParseException{
		byte[] desc = keeper().getData(endpath, true, null);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = json().fromJSON(new String(desc));
		EndpointDescription endpoint = new EndpointDescription(map);
		machine.putRemote(endpath, endpoint);
	}
	
	private ZooKeeper keeper(){
		return manager.getKeeper();
	}
	
	private JSONService json(){
		return manager.getJson();
	}
	
	private class EndpointWatcher implements Watcher {

		public void process(WatchedEvent event) {
			System.out.println("Children event "+event);
			String path = event.getPath();
			
			//Description published by this framework. do nothing
			if (path.startsWith(SEPARATOR+manager.frameworkid)){
				return;
			}
			
			switch (event.getType()) {
			case NodeCreated:
						System.out.println("register Endpoint");
				try {
					processEndpointAdded(event.getPath());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
				break;
			case NodeDataChanged:
				break;
			case NodeDeleted:
				try {
					//Remove endpoint
					if (path.matches("^/([a-zA-Z_0-9]|-|_)+/([a-zA-Z_0-9]|-|_)+\\$")){
						System.out.println("Removed Endpoint");
						machine.removeRemote(path);
					} else if (path.matches("^/([a-zA-Z_0-9]|-|_)+$")) {
						System.out.println("Removed gateway");
						processMachineRemoved(path);
					}
				}catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				break;
			case NodeChildrenChanged:
				try {
					keeper().exists(path, this);
				} catch (Exception e) {
				}
				break;
				
			case None:
			default:
				break;
			}
		}
		
	}
}
