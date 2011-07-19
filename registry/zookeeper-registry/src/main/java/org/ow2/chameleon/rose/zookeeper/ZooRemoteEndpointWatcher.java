package org.ow2.chameleon.rose.zookeeper;

import static org.osgi.service.log.LogService.LOG_ERROR;
import static org.osgi.service.log.LogService.LOG_INFO;
import static org.ow2.chameleon.rose.zookeeper.ZookeeperManager.SEPARATOR;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.ListUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseEndpointDescription;
import org.ow2.chameleon.rose.RoseMachine;

public class ZooRemoteEndpointWatcher {

	private final ZookeeperManager manager;
	private final RoseMachine machine;
	private final String rootNode;
	private ConcurrentHashMap<String, List<String>> registrations;
	private List<String> nodes;
	private boolean running = false;

	public ZooRemoteEndpointWatcher(ZookeeperManager pManager,
			RoseMachine pMachine, String pRootNode) {
		manager = pManager;
		machine = pMachine;
		this.rootNode = pRootNode;
		registrations = new ConcurrentHashMap<String, List<String>>();
		try {
			nodes = keeper().getChildren(rootNode, new MachineWatcher());
			running = true;
			for (String node : nodes) {
				if (node.equals(manager.frameworkid)) {
					continue; // ignore myself
				}
				registrations.put(node, new ArrayList<String>());
				keeper().getChildren(rootNode + SEPARATOR + node,
						new EndpointWatcher(node));
				
			}
		logger().log(LOG_INFO, "Remote endpoints watcher successfully created");	
		} catch (KeeperException e) {
			logger().log(LOG_ERROR, "Can not create a watcher for nodes", e);
		} catch (InterruptedException e) {
			logger().log(LOG_ERROR, "Can not create a watcher for nodes", e);
		}
	}

	public void stop() {
		running = false;
		for (String node : nodes) {
			processMachineRemoved(node);
		}
	}

	private void processMachineRemoved(String node) {

		if (node.equals(manager.frameworkid))
			return;
		for (String endpoint : registrations.get(node)) {
			machine.removeRemote(node + SEPARATOR + endpoint);
		}
		registrations.remove(node);
		logger().log(LOG_INFO, "Machine: "+node+" removed");
	}

	private void processEndpointAdded(String node, String endpoint)
			throws KeeperException, InterruptedException, ParseException {
		byte[] desc = keeper().getData(
				rootNode + SEPARATOR + node + SEPARATOR + endpoint, true, null);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = json().fromJSON(new String(desc));
		EndpointDescription endp = RoseEndpointDescription
				.getEndpointDescription(map);
		machine.putRemote(node + SEPARATOR + endpoint, endp);
		registrations.get(node).add(endpoint);
		logger().log(LOG_INFO, "Added from machine: "+node+" ,endpoint: " +endpoint);
	}

	private void processEndpointRemoved(String node, String endpoint) {
		machine.removeRemote(node + SEPARATOR + endpoint);
		registrations.get(node).remove(endpoint);
		logger().log(LOG_INFO, "Removed from machine: "+node+" ,endpoint: " +endpoint);
	}

	private class MachineWatcher implements Watcher {

		List<String> newNodes;

		/*-----------------------------------*
		 *  Zookeeper Watcher method         *
		 *-----------------------------------*/

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.apache.zookeeper.Watcher#process(org.apache.zookeeper.WatchedEvent
		 * )
		 */

		@SuppressWarnings("unchecked")
		public void process(WatchedEvent event) {
			if (running == false)
				return;
			try {
				newNodes = keeper().getChildren(rootNode, this);
				if (nodes.containsAll(newNodes)) {// node deleted
					for (String node : (List<String>) ListUtils.subtract(nodes,
							newNodes)) {
						processMachineRemoved(node);
					}

				}
				if (newNodes.containsAll(nodes)) {// new node
					for (String node : (List<String>) ListUtils.subtract(
							newNodes, nodes)) {
						logger().log(LOG_INFO, "Machine: "+node+" found");
						registrations.put(node, new ArrayList<String>());
						new EndpointWatcher(node);
					}
				}
				nodes = newNodes;
			} catch (InterruptedException e) {
				logger().log(LOG_ERROR, "Can not get childrens(endpoints) for node", e);
			} catch (KeeperException e) {
				logger().log(LOG_ERROR, "Can not get childrens(endpoints) for node", e);
			}
		}
	}

	private class EndpointWatcher implements Watcher {

		private List<String> endpoints;
		private String node;
		private List<String> newEndpoints;
		private boolean getAll = false; // register all endpoints, run at the
										// beginning of node discovered

		public EndpointWatcher(String node) {
			this.node = node;
			try {
				endpoints = keeper().getChildren(rootNode + SEPARATOR + node,
						false);
				if (endpoints.size() > 0) {
					getAll = true;
					this.process(null);
				}
			} catch (KeeperException e) {
				logger().log(LOG_ERROR, "Can not find a node: "+node, e);
			} catch (InterruptedException e) {
				logger().log(LOG_ERROR, "Can not find a node: "+node, e);
			} 

		}

		/*-----------------------------------*
		 *  Zookeeper Watcher method         *
		 *-----------------------------------*/

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.apache.zookeeper.Watcher#process(org.apache.zookeeper.WatchedEvent
		 * )
		 */

		@SuppressWarnings("unchecked")
		public void process(WatchedEvent event) {
			if (running == false)
				return;
			try {
				if (keeper().exists(rootNode + SEPARATOR + this.node, false) == null)
					return; // node has been deleted

				newEndpoints = keeper().getChildren(
						rootNode + SEPARATOR + node, this);

				if (nodes.containsAll(newEndpoints)) {// endpoint deleted
					for (String endpoint : (List<String>) ListUtils.subtract(
							endpoints, newEndpoints)) {
						processEndpointRemoved(node, endpoint);
					}
				}

				if (newEndpoints.containsAll(endpoints)) {// endpoint added
					if (getAll == true) {
						for (String endpoint : newEndpoints) {
							processEndpointAdded(this.node, endpoint);
						}
					} else {
						for (String endpoint : (List<String>) ListUtils
								.subtract(newEndpoints, endpoints)) {

							processEndpointAdded(this.node, endpoint);
						}
					}
					getAll = false;
				}
			} catch (KeeperException e) {
				logger().log(LOG_ERROR, "Can not find a node",e);
			} catch (InterruptedException e) {
				logger().log(LOG_ERROR, "Can not find a node",e);
			} catch (ParseException e) {
				logger().log(LOG_ERROR, "Can not add an endpoint",e);
			}
		}
	}

	private ZooKeeper keeper() {
		return manager.getKeeper();
	}

	private JSONService json() {
		return manager.getJson();
	}

	private LogService logger() {
		return manager.getLogger();
	}

}
