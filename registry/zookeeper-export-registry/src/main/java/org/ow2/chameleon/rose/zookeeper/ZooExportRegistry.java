package org.ow2.chameleon.rose.zookeeper;

import static org.apache.zookeeper.CreateMode.EPHEMERAL;

import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.registry.ExportedEndpointListener;
import org.ow2.chameleon.rose.registry.ImportRegistryProvisioning;

@Component(name="RoSe.export_registry.zookeeper",propagation=true)
@Provides(specifications=ExportedEndpointListener.class)
public class ZooExportRegistry implements ExportedEndpointListener,Watcher {
	private static final String SEPARATOR="/";
	
	private static final String ROOT = "/rose";
	
	@Property(name="connection",mandatory=true)
	private String connectString;
	
	@Property(name="timeout",mandatory=false)
	private int sessionTimeout;
	
	@SuppressWarnings("unused")
	@Property(name=ENDPOINT_LISTENER_SCOPE,mandatory=false)
	private String filter;

	@Requires(optional=false)
	private ImportRegistryProvisioning registry;
	
	@Requires(optional=false)
	private JSONService json;
	
	private String frameworkid;
	
	private ZooKeeper keeper;
	
	/**
	 * On instance validation call-back (iPOJO).
	 */
	@SuppressWarnings("unused")
	@Validate
	private void start(){
		try {
			keeper = new ZooKeeper(connectString, sessionTimeout, this);
			keeper.create(ROOT, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			System.out.println("YATA");
			//keeper.
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	
	/**
	 * On instance invalidation call-back (iPOJO).
	 */
	@SuppressWarnings("unused")
	@Invalidate
	private void stop(){
		try {
			keeper.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/*----------------------------*
	 * EndpointListener call-back *
	 * (WB pattern)               *
	 *----------------------------*/
	

	public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
		String desc = json.toJSON(endpoint.getProperties());
		try {
			ensurePath(computePath(endpoint), keeper);
			String path = keeper.create(computePath(endpoint), desc.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, EPHEMERAL);
			System.out.println("Node create: "+path);
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
	}

	public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
		try {
			keeper.delete(computePath(endpoint), 0);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (KeeperException e) {
			e.printStackTrace();
		}
		
	}
	
	/*-----------------------------------*
	 *  Zookeeper Watcher method         *
	 *-----------------------------------*/

	/*
	 * (non-Javadoc)
	 * @see org.apache.zookeeper.Watcher#process(org.apache.zookeeper.WatchedEvent)
	 */
	public void process(WatchedEvent event) {
		// TODO Auto-generated method stub
		String path = event.getPath();
		
		//Description published by this framework. do nothing
		if (path.startsWith(ROOT+SEPARATOR+frameworkid+"-")){
			return;
		}
		
		switch (event.getType()) {
		case NodeCreated:
			//Add the endpoint
			try {
				byte[] desc = keeper.getData(path, false, null);
				Map<String, Object> map = json.fromJSON(String.valueOf(desc));
				EndpointDescription endpoint = new EndpointDescription(map);
				registry.put(path, endpoint);
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
	
	private static String computePath(EndpointDescription desc){
		return ROOT+SEPARATOR+desc.getFrameworkUUID()+"-"+desc.getId();
	}
	
	private static void ensurePath(String path, ZooKeeper zk)
			throws KeeperException, InterruptedException {
		StringBuilder current = new StringBuilder();

		String[] tree = path.split("/");
		for (int i = 0; i < tree.length; i++) {
			if (tree[i].length() == 0) {
				continue;
			}

			current.append('/');
			current.append(tree[i]);
			if (zk.exists(current.toString(), false) == null) {
				zk.create(current.toString(), new byte[0], Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}
		}
	}
}

