package org.ow2.chameleon.rose.wui;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.ow2.chameleon.rose.api.Machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.ow2.chameleon.rose.api.Machine.MachineBuilder.machine;

/**
 * User: barjo
 * Date: 05/11/12
 * Time: 14:36
 */
@Component(name="RoSe_Wui")
@Instantiate
public class WuiComp {

    private final Machine myrose;

    private final BundleContext context;

    private final List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();

    //REST Api object, that allows to manage RoSe Machine
    private final WuiMachine wuiMachine;

    //REST Api object, that allows to inspect RoSe elements
    private final WuiInspect wuiInspect;

    public WuiComp(BundleContext context) throws InvalidSyntaxException {
        this.context = context;

        //Create a rose machine that export the REST Api service thx to jersey.
        myrose = machine(context,"wui-machine").create();
        myrose.exporter("RoSe_exporter.jersey").withProperty("jersey.servlet.name","/rose").create();
        myrose.out("("+ Constants.OBJECTCLASS+"="+RESTMachine.class.getName()+")").protocol(Collections.singletonList("jax-rs")).add();
        myrose.out("("+ Constants.OBJECTCLASS+"="+RESTInspect.class.getName()+")").protocol(Collections.singletonList("jax-rs")).add();


        wuiMachine = new WuiMachine(context);
        wuiInspect = new WuiInspect(context);
    }


    @Validate
    private void start() {
        //Register the REST Api that allows to manage RoSe machines
        wuiMachine.register();

        //Register the REST Api that allows to inspect the RoSe elements
        wuiInspect.register();

        myrose.start(); //Start the RoSe machine, export the WUI with jersey
    }

    @Invalidate
    private void stop() {
        myrose.stop(); //Stop the RoSe machine, destroy the WUI

        wuiMachine.unRegister();
        wuiInspect.unRegister();
    }
}