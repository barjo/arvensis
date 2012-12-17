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

package fr.liglab.adele.cilia.proxies.rose;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.ow2.chameleon.rose.AbstractImporterComponent;
import org.ow2.chameleon.rose.ImporterService;
import org.ow2.chameleon.rose.RoseMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.felix.ipojo.Factory.VALID;
import static org.osgi.framework.Constants.OBJECTCLASS;

/**
 * Service importer for tcp/ip protocol <br>
 * 
 * 
 * @author Denis Morand
 */
public class ProxyDeviceImporter extends AbstractImporterComponent implements ImporterService{
	private static final Logger logger = LoggerFactory.getLogger("cilia.rose.proxies");

	private static final String FACTORY_FILTER = "(" + OBJECTCLASS + "="
			+ Factory.class.getName() + ")(factory.state=" + VALID + ")";

	private RoseMachine roseMachine;
	BundleContext m_bundleContext;

	public ProxyDeviceImporter(BundleContext context) throws InvalidSyntaxException {
		super();
		m_bundleContext = context;
	}

	/*
	 * list of currently supported protocol
	 */
	public List getConfigPrefix() {
		List list = new ArrayList();
		list.add("tcp.ip");
		return list;
	}

	protected ServiceRegistration createProxy(EndpointDescription description,
			Map<String, Object> extraProperties) {

		Hashtable props = new Hashtable(description.getProperties());
		props.putAll(extraProperties);
		/* Retreive the factory name */
		String factoryName = (String) props.get("service.factory");
		StringBuilder sb = new StringBuilder("(&");
		sb.append(FACTORY_FILTER);
		sb.append("(factory.name=").append(factoryName).append("))");

		try {
			/* Look for a ipojo Factory */
			ServiceReference[] refs = m_bundleContext.getAllServiceReferences(
					Factory.class.getName(), sb.toString());
			if (refs != null) {
				Factory factory = (Factory) m_bundleContext.getService(refs[0]);
				addProperties(props,description);
				ComponentInstance instance = factory.createComponentInstance(props);
				if (instance != null) {
					ServiceRegistration sr = new DeviceService(instance);
					sr.setProperties(props);
					return sr;
				} else {
					logger.error("Proxy creation error");
				}
			} else {
				logger.error("Factory not found");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	
	public  void addProperties(Map props, EndpointDescription description) {
		props.put("managed.service.pid", description.getId()) ;
		String value = (String) props.get("rank.value");
		if (value != null) {
			try {
				Integer.parseInt(value);
				props.put(org.osgi.framework.Constants.SERVICE_RANKING, value);
			} catch (NumberFormatException e) {
				logger.error("Service ranking property must be an integer string format");
			}
		}
		
	}
	
	protected void destroyProxy(EndpointDescription description,
			ServiceRegistration registration) {
		logger.debug("Endoint destroyed ,ID=" + description.getId());
		registration.unregister();
	}

	protected LogService getLogService() {
		return null;
	}

	public RoseMachine getRoseMachine() {
		return roseMachine;
	}

	protected void start() {
		super.start();
		logger.debug("Proxy importer started");
	}

	protected void stop() {
		super.stop();
		logger.debug("Proxy importer stopped");
	}

	/**
	 * A wrapper for ipojo Component instances
	 */
	class DeviceService implements ServiceRegistration {

		ComponentInstance instance;

		public DeviceService(ComponentInstance instance) {
			super();
			this.instance = instance;
			logger.debug("Device Proxy Service create=" + instance.getInstanceName());
		}

		public ServiceReference getReference() {
			try {
				ServiceReference[] references = instance.getContext()
						.getServiceReferences(instance.getClass().getCanonicalName(),
								"(instance.name=" + instance.getInstanceName() + ")");
				if (references != null) {
					logger.debug("Device Proxy Service , getServiceReferences[0]="
							+ references[0].getClass().getName());
					return references[0];
				} else
					logger.error("Device proxy service, get Service reference=null");
			} catch (InvalidSyntaxException e) {
				logger.error("Proxy instance error" + e.getStackTrace().toString());
			}
			return null;
		}

		public void setProperties(Dictionary properties) {
			if (logger.isDebugEnabled()) {
				StringBuffer sb = new StringBuffer("Device Proxy [");
				sb.append(instance.getInstanceName()).append("] ");
				sb.append("properties = ").append(properties.toString());
				logger.debug(sb.toString());
			}
			instance.reconfigure(properties);
		}

		public void unregister() {
			logger.debug("Device Proxy Service unregister :" + instance.getInstanceName());
			instance.dispose();
		}
	}

}
