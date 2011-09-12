package org.ow2.chameleon.rose;

import java.util.ArrayList;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;

public class RoseEndpointDescription {

	@SuppressWarnings("unchecked")
	public static EndpointDescription getEndpointDescription(Map<String, Object> map){
		
		if (map.get(Constants.OBJECTCLASS) instanceof ArrayList<?>){
			map.put(Constants.OBJECTCLASS, ((ArrayList<String>)map.get(Constants.OBJECTCLASS)).toArray(new String[0]));			
		}
		
		if (map.get(RemoteConstants.ENDPOINT_SERVICE_ID) instanceof Integer){
			Integer id =(Integer) map.get((RemoteConstants.ENDPOINT_SERVICE_ID)) ;
			map.put(RemoteConstants.ENDPOINT_SERVICE_ID, id.longValue());
		}
		return new EndpointDescription(map);
		
	}
	
}
