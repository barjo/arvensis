    package org.ow2.chameleon.rose.websocket;

    import org.apache.felix.ipojo.annotations.*;
    import org.glassfish.grizzly.websockets.WebSocketApplication;
    import org.glassfish.grizzly.websockets.WebSocketEngine;
    import org.osgi.framework.BundleContext;
    import org.osgi.service.http.HttpService;
    import org.osgi.service.http.NamespaceException;
    import org.osgi.service.log.LogService;

    import javax.servlet.ServletConfig;
    import javax.servlet.ServletException;
    import javax.servlet.http.HttpServlet;

    /**
     * Simple component that register a WebSocketApplication, the WebSocketEndpointDiscoApp
     * allows to publish the local EndpointDescription through a websocket.
     * @author barjo
     * @see WebSocketEndpointDiscoApp
     */
    @Component(name="RoSe_websocket_publication")
    @Instantiate
    public class WebSocketComp extends HttpServlet{

        @Requires(optional = true)
        private LogService logger;

        @Requires
        private HttpService http;

        @Property(name = "root.name",value = "/disco")
        private String rootname;

        private final WebSocketApplication app;

        public WebSocketComp(BundleContext context){
            app=new WebSocketEndpointDiscoApp(context);
        }


        /**
         * Register the WebSocketApplication.
         * @param config
         * @throws ServletException
         */
        @Override
        public void init(ServletConfig config) throws ServletException {
            super.init(config);
            WebSocketEngine.getEngine().register(app);

        }


        /**
         * Unregister the WebSocketApplication when the servlet is destroyed
         */
        @Override
        public void destroy() {
            WebSocketEngine.getEngine().unregister(app);
            logger.log(LogService.LOG_INFO,"The WebSocket app has been unregistered");
            super.destroy();
        }

        /**
         * Start the component, register itself as a servlet through the HttpService.
         */
        @Validate
        private void start(){
            logger.log(LogService.LOG_INFO,"RoSe WebSocket registry starting.");
            try {
                http.registerServlet(rootname,this,null,null);
            } catch (ServletException e) {
                logger.log(LogService.LOG_ERROR,"Cannot register the websocket",e);
            } catch (NamespaceException e) {
                logger.log(LogService.LOG_ERROR,"Cannot register the websocket",e);
            }
        }

        /**
         * Unregister itself through the HttpService.
         */
        @Invalidate
        private void stop(){
            logger.log(LogService.LOG_INFO,"RoSe WebSocket registry stopping.");
            http.unregister(rootname);
        }

    }
