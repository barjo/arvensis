package org.ow2.chameleon.rose.rest.provider;

import com.sun.jersey.core.spi.component.ioc.IoCProxiedComponentProvider;


public class ProxiedComponentProvider implements IoCProxiedComponentProvider {
    
    public ProxiedComponentProvider(Class<?> pClass) {
    }

    public Object getInstance() {
        throw new UnsupportedOperationException("the method getInstance is not supported for a ProxiedComponent");
    }

    public Object proxy(Object instance) {
        return instance;
    }

}
