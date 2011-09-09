package org.ow2.chameleon.rose.rest;

import java.util.Map;

import javax.servlet.ServletException;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.container.servlet.WebConfig;


public class JerseyServletBridge extends ServletContainer{

    private static final long serialVersionUID = -1399500555655064091L;
    
    private final EndpointCreator providerFactory;
    
    
    public JerseyServletBridge(EndpointCreator pProviderFactory) {
        super();
        providerFactory = pProviderFactory;
    }

    @Override
    protected ResourceConfig getDefaultResourceConfig(Map<String, Object> props, WebConfig wc) throws ServletException {
        return providerFactory.getResourceConfig();
    }
    
    @Override
    protected void initiate(ResourceConfig rsc, WebApplication webApp) {
        webApp.initiate(rsc,providerFactory);
    }
}
