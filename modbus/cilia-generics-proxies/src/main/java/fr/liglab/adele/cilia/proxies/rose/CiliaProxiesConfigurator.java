package fr.liglab.adele.cilia.proxies.rose;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.ow2.chameleon.rose.DynamicExporter;
import org.ow2.chameleon.rose.DynamicImporter;
import org.ow2.chameleon.rose.DynamicImporterCustomizer;
import org.ow2.chameleon.rose.ImporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CiliaProxiesConfigurator {
	private static final Logger logger = LoggerFactory.getLogger("cilia.rose.proxies");
	private final DynamicExporter dynamicExporterWS;
	private DynamicImporter dynamicImporterWS, dynamicImporterDevice;

	public CiliaProxiesConfigurator(BundleContext context) throws InvalidSyntaxException {

		dynamicExporterWS = new DynamicExporter.Builder(context,
				"(service.exported=fr.liglab.adele.webservice)").protocol(
				getProtocolWebService()).build();

		dynamicImporterWS = new DynamicImporter.Builder(context,
				"(service.imported=fr.liglab.adele.webservice)").protocol(
				getProtocolWebService()).build();

		dynamicImporterDevice = new DynamicImporter.Builder(context,
				"(service.imported=fr.liglab.adele.device)")
				.protocol(getProtocolDevice()).customizer(new WrapperImporter()).build();

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
		dynamicExporterWS.start();
		dynamicImporterWS.start();
		dynamicImporterDevice.start();
	}

	protected void stop() {
		dynamicExporterWS.stop();
		dynamicImporterWS.stop();
		dynamicImporterDevice.stop();
	}

	/* OSGI */
	private class WrapperImporter implements DynamicImporterCustomizer {

		public WrapperImporter() {
		}

		public ImportReference[] getImportReferences()
				throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		public Object doImport(ImporterService importer, EndpointDescription description,
				Map<String, Object> properties) {
			ImportRegistration registration = importer.importService(description,
					properties);
			return registration;
		}

		public void unImport(ImporterService importer, EndpointDescription description,
				Object registration) {
			ImportRegistration regis = (ImportRegistration) registration;
			regis.close();
		}


	}

}
