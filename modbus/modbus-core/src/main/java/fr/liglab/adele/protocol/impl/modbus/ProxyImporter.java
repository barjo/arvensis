/*
 * Copyright Adele Team LIG
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.liglab.adele.protocol.impl.modbus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.ow2.chameleon.rose.AbstractImporterComponent;
import org.ow2.chameleon.rose.DynamicImporter;
import org.ow2.chameleon.rose.DynamicImporterCustomizer;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.RoseMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service importer for Service Modbus <br>
 * Standard Modbus or <br>
 * if the device responds to Modbus request 43/14, the Service Modbus is added
 * by identification properties<br>
 * 
 * @author Denis Morand
 * 
 */
public class ProxyImporter extends AbstractImporterComponent {
	private static final Logger logger = LoggerFactory.getLogger("modbus.proxy");

	private Factory modbusFactory;
	private RoseMachine roseMachine;
	private final DynamicImporter dynaimp;
	private final ProxyRanker ranker;
	private String m_urlProperties;

	public ProxyImporter(BundleContext context) throws InvalidSyntaxException {
		ranker = new ProxyRanker();
		dynaimp = new DynamicImporter.Builder(context,
				"(service.imported=fr.liglab.adele.modbus.tcp)")
				.protocol(getConfigPrefix()).customizer(ranker).build();
		logger.trace("Proxy importer instancied");
	}

	public List getConfigPrefix() {
		List list = new ArrayList();
		list.add("fr.liglab.adele.modbus.tcp");
		return list;
	}

	protected ServiceRegistration createProxy(EndpointDescription description,
			Map<String, Object> extraProperties) {

		Hashtable props = new Hashtable(description.getProperties());
		props.putAll(extraProperties);
		ComponentInstance instance;
		try {
			instance = modbusFactory.createComponentInstance(props);
			if (instance != null) {
				logger.info("Create Proxy Modbus :" + props.get("device.ip.address")
						+ ":" + props.get("device.ip.port"));
				ServiceRegistration sr = new ModbusDeviceService(instance);
				return sr;
			} else {
				logger.error("Proxy creation error, modbus factory return null");
			}
		} catch (Exception ex) {
			logger.error("Proxy creation error" + ex.getStackTrace().toString());
		}

		return null;
	}

	protected void destroyProxy(EndpointDescription description,
			ServiceRegistration registration) {
		if (logger.isInfoEnabled()) {
			Dictionary props = new Hashtable(description.getProperties());
			logger.info("Destroy Proxy Modbus :" + props.get("device.ip.address") + ":"
					+ props.get("device.ip.port"));
		}
		registration.unregister();
	}

	protected LogService getLogService() {
		return null;
	}

	protected RoseMachine getRoseMachine() {
		return roseMachine;
	}

	protected void start() {
		super.start();
		ranker.start();
		dynaimp.start();
		logger.debug("Proxy importer started");
	}

	protected void stop() {
		super.stop();
		dynaimp.stop();
		ranker.stop();
		logger.debug("Proxy importer stopped");
	}

	/**
	 * A wrapper for ipojo Component instances
	 */
	class ModbusDeviceService implements ServiceRegistration {

		ComponentInstance instance;

		public ModbusDeviceService(ComponentInstance instance) {
			super();
			this.instance = instance;
			logger.debug("Modbus Proxy Service create=" + instance.getInstanceName());
		}

		public ServiceReference getReference() {
			try {
				ServiceReference[] references = instance.getContext()
						.getServiceReferences(instance.getClass().getCanonicalName(),
								"(instance.name=" + instance.getInstanceName() + ")");
				if (references != null) {
					logger.debug("Modbus Proxy Service , getServiceReferences[0]="
							+ references[0].getClass().getName());
					return references[0];
				} else
					logger.error("Modbus proxy service, get Service reference=null");
			} catch (InvalidSyntaxException e) {
				logger.error("Proxy instance error" + e.getStackTrace().toString());
			}
			return null;
		}

		public void setProperties(Dictionary properties) {
			if (logger.isDebugEnabled()) {
				StringBuffer sb = new StringBuffer("Modbus Proxy [");
				sb.append(instance.getInstanceName()).append("] ");
				sb.append("properties = ").append(properties.toString());
				logger.debug(sb.toString());
			}
			instance.reconfigure(properties);
		}

		public void unregister() {
			logger.debug("Modbus Proxy Service unregister :" + instance.getInstanceName());
			instance.dispose();
		}
	}

	/* OSGI Ranker */
	private class ProxyRanker implements DynamicImporterCustomizer {
		private Properties devicesProps;

		public ProxyRanker() {
		}

		public void start() {
			logger.info("Proxy ranker started");
			devicesProps = new Properties();
			try {
				devicesProps.load(new URL(m_urlProperties).openStream());
				logger.debug("Properties read " + devicesProps.toString());
			} catch (MalformedURLException e) {
				logger.error("Invalid URL");
			} catch (IOException e) {
				logger.debug("file " + m_urlProperties + " not existing");
			}

		}

		public void stop() {
		}

		public ImportReference[] getImportReferences()
				throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		public Object doImport(ImporterService importer, EndpointDescription description,
				Map<String, Object> properties) {
			setRank(properties, description);
			ImportRegistration registration = importer.importService(description,
					properties);
			ImportReference iref = registration.getImportReference();
			return registration;
		}

		public void unImport(ImporterService importer, EndpointDescription description,
				Object registration) {
			ImportRegistration regis = (ImportRegistration) registration;
			regis.close();
		}

		public void setRank(Map properties, EndpointDescription description) {
			Map props = description.getProperties();
			String key = (String) props.get("device.ip.address");
			if (key != null) {
				String value = devicesProps.getProperty(key);
				try {
					Integer.parseInt(value);
					properties.put(org.osgi.framework.Constants.SERVICE_RANKING, value);
					logger.debug("device=" + key + " 'service.ranking=" + value);
				} catch (NumberFormatException e) {
					logger.error("Malformed number in device properties file " + key
							+ " value =" + value);
				}
			}
		}

	}
}
