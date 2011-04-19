package org.ow2.chameleon.rose.zookeeper;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.registry.ExportedEndpointListener;

@Component(name="RoSe.export_registry.zookeeper",propagation=true)
@Instantiate(name="RoSe.export_registry.zookeeper-default")
@Provides(specifications=ExportedEndpointListener.class)
public class ZooExportRegistry implements ExportedEndpointListener {

	@SuppressWarnings("unused")
	@Property(name=ENDPOINT_LISTENER_SCOPE,mandatory=false)
	private String filter;
	
	/**
	 * On instance validation call-back (iPOJO).
	 */
	@SuppressWarnings("unused")
	@Validate
	private void start(){
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

