package org.ow2.chameleon.rose.configurator;

import org.ow2.chameleon.rose.api.InConnection;
import org.ow2.chameleon.rose.api.Instance;
import org.ow2.chameleon.rose.api.Machine;
import org.ow2.chameleon.rose.api.OutConnection;

import java.util.Collection;

/**
 * User: barjo
 * Date: 29/10/12
 * Time: 11:21
 */
public class MachineConfiguration {
    private final Collection<OutConnection> outs;
    private final Collection<InConnection> ins;
    private final Collection<Instance> instances;
    private final String machineId;

    public MachineConfiguration(Machine machine){
        outs = machine.getOuts();
        ins = machine.getIns();
        instances = machine.getInstances();
        machineId = machine.getId();
    }

    public Collection<OutConnection> getOuts() {
        return outs;
    }

    public Collection<InConnection> getIns() {
        return ins;
    }

    public Collection<Instance> getInstances() {
        return instances;
    }

    public String getMachineId() {
        return machineId;
    }
}
