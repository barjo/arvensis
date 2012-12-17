package org.ow2.chameleon.rose.configurator.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.ow2.chameleon.rose.configurator.Configurator;

import java.io.File;

/**
 * User: barjo
 * Date: 29/10/12
 * Time: 13:32
 */
public class ConfiguratorTest {
    private static String CONF_1 = " { \"machine\" : { \"id\" : \"toto\", \"connection\" : [ { \"out\" : {\"service_filter\" : \"(objectClass=org.toto.Test)\" } } ] } }  ";

    @Mock
    private BundleContext context;

    //tested Object
    private Configurator configurator;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this); //initialize the object with mocks annotations
        configurator = new Configurator(context);

    }

    @Test
    public void testCanHandle() throws Exception {
        File good = File.createTempFile("rose-conf-",".json");
        File bad = File.createTempFile("rose-conff",".json");
        File badd = File.createTempFile("rose-conf-",".conf");
        Assert.assertTrue(configurator.canHandle(good));
        Assert.assertFalse(configurator.canHandle(bad));
        Assert.assertFalse(configurator.canHandle(badd));
    }
}
