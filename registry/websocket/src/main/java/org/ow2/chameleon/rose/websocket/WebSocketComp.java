    package org.ow2.chameleon.rose.websocket;

    import org.apache.felix.ipojo.annotations.*;
    import org.glassfish.grizzly.websockets.WebSocketApplication;
    import org.glassfish.grizzly.websockets.WebSocketEngine;
    import org.osgi.framework.BundleContext;
    import org.osgi.service.http.HttpService;
    import org.osgi.service.log.LogService;

    /**
     * Simple component that register a WebSocketApplication, the WebSocketEndpointDiscoApp
     * allows to publish the local EndpointDescription through a websocket.
     * @author barjo
     * @see WebSocketEndpointDiscoApp
     */
    @Component(name="RoSe_websocket_publication")
    @Instantiate
    public class WebSocketComp {

        @Requires(optional = true)
        private LogService logger;

        @Requires
        private HttpService http;

        private final WebSocketApplication app;

        public WebSocketComp(BundleContext context){
            app=new WebSocketEndpointDiscoApp(context);
        }


        /**
         * Start the component, register the app.
         */
        @Validate
        private void start(){
            logger.log(LogService.LOG_INFO, "RoSe WebSocket registry starting.");
            WebSocketEngine.getEngine().register(app);
        }

        /**
         * Unregister the app.
         */
        @Invalidate
        private void stop(){
            logger.log(LogService.LOG_INFO, "RoSe WebSocket registry stopping.");
            WebSocketEngine.getEngine().unregister(app);
        }

    }
