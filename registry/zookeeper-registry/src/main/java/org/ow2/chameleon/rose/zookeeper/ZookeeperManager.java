package org.ow2.chameleon.rose.zookeeper;

import static org.apache.zookeeper.CreateMode.EPHEMERAL;
import static org.osgi.service.log.LogService.LOG_INFO;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.osgi.service.remoteserviceadmin.EndpointListener.ENDPOINT_LISTENER_SCOPE;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.json.JSONService;
import org.ow2.chameleon.rose.registry.ImportRegistryProvisioning;
import org.ow2.chameleon.rose.util.DefaultLogService;

/**
 * TODO Handle concurrency.
 * @author barjo
 */
@Component(name="RoSe.registry.zookeeper",propagation=true)
public class ZookeeperManager implements Watcher {
	public static final String SEPARATOR="/";
	
	@Property(name="connection",mandatory=true)
	private String connectString;
	
	@Property(name="timeout",mandatory=false)
	private int sessionTimeout;
	
	@Property(name=ENDPOINT_LISTENER_SCOPE,mandatory=false)
	private String filter;

	@Requires(optional=false)
	private ImportRegistryProvisioning registry;
	
	@Requires(optional=false)
	private JSONService json;
	
	@Requires(optional=true,defaultimplementation=DefaultLogService.class)
	private LogService logger;
	
	private BundleContext context;
	
	
	public String frameworkid;
	
	private ZooKeeper keeper;
	
	private RoseLocalEndpointListener listener;
	private ZooRemoteEndpointWatcher provisioner;
	
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
			//connect the client.
			keeper = new ZooKeeper(connectString, sessionTimeout, this);
			
			//create a node for the framework id.
			createFrameworkNode();
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
		switch (event.getState()) {
		case Expired: // TODO handle expired (i.e create a new connection)
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
		provisioner.stop();
		listener.destroy();
		provisioner = null;
		listener = null;
	}
	
	/**
	 * Create the {@link RoseLocalEndpointListener} and the {@link ZooRemoteEndpointWatcher}
	 */
	private void createListenerAndProvisioner(){
		provisioner = new ZooRemoteEndpointWatcher(this, registry);
		listener = new RoseLocalEndpointListener(this,filter,context);
	}
	
	/**
	 * Create the node associated to the framework id. 
	 * Destroy it if it was previously defined.
	 */
	private void createFrameworkNode(){
		try{
			Stat stat = keeper.exists(SEPARATOR+frameworkid, false);
			if (stat==null){
				keeper.create(SEPARATOR+frameworkid, new byte[0], Ids.OPEN_ACL_UNSAFE, EPHEMERAL);
			}else {
				//server crash, we just reconnect
				keeper.delete(SEPARATOR+frameworkid, -1);
				keeper.create(SEPARATOR+frameworkid, new byte[0], Ids.OPEN_ACL_UNSAFE, EPHEMERAL); //re create the node
			}
			getLogger().log(LOG_WARNING, "A zookeeper node has been successfully created for this framework, node: "+SEPARATOR+frameworkid);
		}catch(Exception ke){
			getLogger().log(LOG_WARNING, "Cannot create a zookeeper node for this framework.",ke);
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
		return SEPARATOR+desc.getFrameworkUUID()+SEPARATOR+desc.getId();
	}
}

