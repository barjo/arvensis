package org.ow2.chameleon.rose.zookeeper;

import static org.apache.zookeeper.CreateMode.PERSISTENT;
import static org.osgi.service.log.LogService.LOG_DEBUG;
import static org.osgi.service.log.LogService.LOG_INFO;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.osgi.service.remoteserviceadmin.EndpointListener.ENDPOINT_LISTENER_SCOPE;

import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.RoseMachine;
import org.ow2.chameleon.rose.util.DefaultLogService;

/**
 * TODO Handle concurrency.
 * @author barjo
 */
@Component(name="RoSe_registry.zookeeper",propagation=true)
public class ZookeeperManager implements Watcher {
	public static final String SEPARATOR="/";
	
	@Property(name="connection",mandatory=true)
	private String connectString;
	
	@Property(name="timeout",mandatory=false,value="40000")
	private int sessionTimeout;
	
	@Property(name=ENDPOINT_LISTENER_SCOPE,mandatory=false)
	private String filter;

	@Requires(optional=false,id="rose.machine")
	private RoseMachine machine;
	
	@Requires(optional=false)
	private JSONService json;
	
	@Requires(optional=true,defaultimplementation=DefaultLogService.class)
	private LogService logger;
	
	private BundleContext context;
	
	
	public String frameworkid;
	
	private ZooKeeper keeper;
	
	private RoseLocalEndpointListener listener;
	private ZooRemoteEndpointWatcher provisioner;
	
	private String host="";
	
	private static String rootNode="";
	
	public ZookeeperManager(BundleContext pContext) {
		context=pContext;
	}
	
	
	/**
	 * On instance validation call-back (iPOJO).
	 */
	@SuppressWarnings("unused")
	@Validate
	private void start() {

		try {
			//Set the framework id
			frameworkid = machine.getId();
			getRootNode();
			//connect the client.
			keeper = new ZooKeeper(host, sessionTimeout, this);

		} catch (Exception e) {
			getLogger().log(LogService.LOG_ERROR, "Cannot start zookeeper registry bridge.",e);
		}

	}


	/**
	 * On instance invalidation call-back (iPOJO).
	 */
	@SuppressWarnings("unused")
	@Invalidate
	private void stop(){
		try {
			if(keeper!=null){
				destroyListenerAndProvider();
				destroyFrameworkNode();
				keeper.close();
				
			}
		} catch (InterruptedException e) {
			getLogger().log(LogService.LOG_ERROR, "Cannot stop properly the zookeeper registry bridge.",e);
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
		System.out.println("Event received: "+event);
		getLogger().log(LOG_DEBUG, "An event has been received"+event);
		if (event.getType()==EventType.NodeDeleted)return;
		switch (event.getState()) {
		case Expired: // TODO handle expired (i.e create a new connection)
			break;
		case Disconnected:
			getLogger().log(LOG_WARNING, "The zookeeper client has been disconnected.");
			// The client has been disconnected for some reason, destroy the
			// Listener and the provisioner
			destroyListenerAndProvider();
			break;
		case SyncConnected:
			getLogger().log(LOG_INFO, "The zookeeper client has been connected.");
			// we have been reconnected, recreate the framework node and the
			// Listener and the provisioner
			createFrameworkNode();
			createListenerAndProvisioner();
			break;
		default:
			break;
		}
	}
	


	/**
	 * Destroy the {@link RoseLocalEndpointListener} and the {@link ZooRemoteEndpointWatcher}.
	 */
	private void destroyListenerAndProvider(){
		if (provisioner != null){
			provisioner.stop();
			provisioner = null;
		}
		
		if (listener != null){
			listener.destroy();
			listener = null;
		}
	}
	
	/**
	 * Create the {@link RoseLocalEndpointListener} and the {@link ZooRemoteEndpointWatcher}
	 */
	private void createListenerAndProvisioner(){
		provisioner = new ZooRemoteEndpointWatcher(this, machine,rootNode);
		listener = new RoseLocalEndpointListener(this,filter,context);
	}
	
	/**
	 * Create the node associated to the framework id. 
	 * Destroy it if it was previously defined.
	 */
	private void createFrameworkNode(){
		try{
			byte data[] = {0}; 
			Stat stat = keeper.exists(rootNode+SEPARATOR+frameworkid, false);
			
			if (stat==null){
				keeper.create(rootNode+SEPARATOR+frameworkid, data, Ids.OPEN_ACL_UNSAFE, PERSISTENT);
			}else {
				//delete bad children !
				List<String> childs = keeper.getChildren(rootNode+SEPARATOR+frameworkid, false);
				for (String child : childs) {
					try{
						keeper.delete(rootNode+SEPARATOR+frameworkid+SEPARATOR+child, -1);
					}
					catch(KeeperException e){
						//ignore
					}
				}
			}
			getLogger().log(LOG_DEBUG, "A zookeeper node has been successfully created for this framework, node: "+SEPARATOR+frameworkid);
		}catch(Exception ke){
			getLogger().log(LOG_WARNING, "An exception occured while creating a zookeeper node for this framework.",ke);
		}
	}
	
	/**
	 * Destroy this framework node.
	 */
	private void destroyFrameworkNode(){
		try {
			keeper.delete(rootNode+SEPARATOR+frameworkid, -1);
		} catch (Exception e) {
			getLogger().log(LOG_WARNING, "An exception occured while deleting the zookeeper node for this framework.",e);
		}
	}
	
	
	public ZooKeeper getKeeper() {
		return keeper;
	}

	public JSONService getJson() {
		return json;
	}
	
	public LogService getLogger(){
		return logger;
	}
	
	public static String computePath(EndpointDescription desc){
		return rootNode+SEPARATOR+desc.getFrameworkUUID()+SEPARATOR+desc.getId();
	}
	
	private void getRootNode() {
		String[] tmp = connectString.split(SEPARATOR, 2);
		host=tmp[0];
		if (tmp.length>1) rootNode=SEPARATOR+tmp[1]; 
	}
}

