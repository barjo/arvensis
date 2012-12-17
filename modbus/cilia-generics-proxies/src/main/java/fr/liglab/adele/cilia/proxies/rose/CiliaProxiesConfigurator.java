package fr.liglab.adele.cilia.proxies.rose;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.ow2.chameleon.rose.api.Machine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.ow2.chameleon.rose.api.Machine.MachineBuilder.machine;

public class CiliaProxiesConfigurator {
	private static final Logger logger = LoggerFactory.getLogger("cilia.rose.proxies");
    private final Machine rose;

	public CiliaProxiesConfigurator(BundleContext context) throws InvalidSyntaxException {

        rose= machine(context,"rose-modbus").create();

        //Discovery
        rose.instance("WebService_HttpPing.inquiry").create();
        rose.instance("Modbus/TCP.discovery").create();

        //Importer
        rose.importer("Cilia_Importer.device").create();
        rose.exporter("RoSe_importer.cxf").create();

        //Exporter
        rose.exporter("RoSe_exporter.cxf").withProperty("cxf.servlet.name","modbus/ws").create();

        //Connections
        rose.out("(service.exported=fr.liglab.adele.webservice)").protocol(getProtocolWebService()).add();
        rose.in("(service.imported=fr.liglab.adele.webservice)").protocol(getProtocolWebService()).add();
        rose.in("(service.imported=fr.liglab.adele.device)").protocol(getProtocolDevice()).add();
    }

	/**
	 * List of protocols actually managed
	 */
	public List getProtocolWebService() {
		List list = new ArrayList();
		list.add("org.apache.cxf");
		list.add("cxf");
		list.add("ws");
		list.add("jax-ws");
		return list;
	}

	public List getProtocolDevice() {
		List list = new ArrayList();
		list.add("tcp.ip");
		return list;
	}

	protected void start() {
		rose.start();
	}

	protected void stop() {
		rose.stop();
	}

}
