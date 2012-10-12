package org.ow2.chameleon.rose.websocket;

import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.osgi.application.Framework;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.ow2.chameleon.rose.RoseMachine;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * User: barjo
 * Date: 12/10/12
 * Time: 10:03
 */
public class WebSocketEndpointDiscoApp extends WebSocketApplication {

    private final BundleContext context;
    private final Map<WebSocket,ServiceRegistration> regs;


    public WebSocketEndpointDiscoApp(BundleContext pContext) {
        context = pContext;
        regs = new HashMap<WebSocket, ServiceRegistration>();
    }

    @Override
    public boolean isApplicationRequest(HttpRequestPacket request) {
            return true;
    }

    /**
     * @param socket
     */
    @Override
    public void onConnect(WebSocket socket) {
        Hashtable<String,Object> properties = new Hashtable<String,Object>();
        //only discover the local endpointdescription
        properties.put(RoseMachine.ENDPOINT_LISTENER_INTEREST,RoseMachine.EndpointListerInterrest.LOCAL);

        ServiceRegistration sr = context.registerService(EndpointListener.class.getName(),new EndpointDispatcher(socket),properties);
        regs.put(socket,sr);
    }

    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
        if (regs.containsKey(socket)){
            regs.remove(socket).unregister();
        }

        super.onClose(socket, frame);
    }
}