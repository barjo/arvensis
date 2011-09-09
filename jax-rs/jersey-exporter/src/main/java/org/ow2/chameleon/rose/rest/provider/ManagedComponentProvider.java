package org.ow2.chameleon.rose.rest.provider;

import static com.sun.jersey.core.spi.component.ComponentScope.Singleton;

import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.core.spi.component.ioc.IoCManagedComponentProvider;

/**
 * If an instance of ManagedComponentProvider is returned then the
 * component is fully managed by the underlying IoC framework, which
 * includes managing the construction, injection and destruction according
 * to the life-cycle declared in the IoC framework's semantics.
 */
public class ManagedComponentProvider implements IoCManagedComponentProvider {
    
    private final Object instance;

    public ManagedComponentProvider(Object pInstance) {
        instance = pInstance;
    }

    /**
     * Since the instance is an OSGi service, this is a singleton pattern !
     */
    public ComponentScope getScope() {
        return Singleton;
    }

    public Object getInjectableInstance(Object o) {
        return instance;
    }

    public Object getInstance() {
        return instance;
    }
}