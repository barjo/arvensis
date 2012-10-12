package org.ow2.chameleon.rose.websocket;

import org.glassfish.grizzly.websockets.WebSocket;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.EndpointListener;

/**
 * User: barjo
 * Date: 12/10/12
 * Time: 13:16
 */
public class EndpointDispatcher implements EndpointListener{
    private final WebSocket wsocket;

    public EndpointDispatcher(WebSocket pWebsocket){
        wsocket=pWebsocket;
    }

    @Override
    public void endpointAdded(EndpointDescription endpointDescription, String s) {
       wsocket.send(createMessage(endpointDescription.toString(),DescriptionType.ADDED));
    }

    @Override
    public void endpointRemoved(EndpointDescription endpointDescription, String s) {
        wsocket.send(createMessage(endpointDescription.toString(),DescriptionType.REMOVED));
    }

    private static String createMessage(String description,DescriptionType type){
        StringBuilder builder = new StringBuilder("{\n\t\"type\" : ");

        switch (type){
            case ADDED:
                builder.append("\"added\"");
            break;
            case REMOVED:
                builder.append("\"removed\"");
            break;
        }
        builder.append(",\n\t\"description\" : ");
        builder.append(description);
        builder.append("}");

        return builder.toString();
    }

    private enum DescriptionType   {
       ADDED,REMOVED;
    }

}
