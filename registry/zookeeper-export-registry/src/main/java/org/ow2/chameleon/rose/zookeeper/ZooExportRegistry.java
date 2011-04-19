package org.ow2.chameleon.rose.zookeeper;

import java.io.IOException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.registry.ExportedEndpointListener;

@Component(name="RoSe.export_registry.zookeeper",propagation=true)
@Instantiate(name="RoSe.export_registry.zookeeper-default")
@Provides(specifications=ExportedEndpointListener.class)
public class ZooExportRegistry implements ExportedEndpointListener {
	
	private String connectString;
	
	private int sessionTimeout;
	
	

	@SuppressWarnings("unused")
	@Property(name=ENDPOINT_LISTENER_SCOPE,mandatory=false)
	private String filter;
	
	/**
	 * On instance validation call-back (iPOJO).
	 */
	@SuppressWarnings("unused")
	@Validate
	private void start(){
		try {
			ZooKeeper keeper = new ZooKeeper("", 1200, null);
			
			//keeper.
		} catch (IOException e) {
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
		
	}
	
	/*----------------------------*
	 * EndpointListener call-back *
	 * (WB pattern)               *
	 *----------------------------*/
	

	public void endpointAdded(EndpointDescription endpoint, String matchedFilter) {
		// TODO Auto-generated method stub
		
	}

	public void endpointRemoved(EndpointDescription endpoint, String matchedFilter) {
		// TODO Auto-generated method stub
		
	}
	
	
}

