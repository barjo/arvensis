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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.ow2.chameleon.rose.AbstractImporterComponent;
import org.ow2.chameleon.rose.DynamicImporter;
import org.ow2.chameleon.rose.RoseMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service importer for Service Modbus <br>
 * Standard Modbus or <br>
 * if the device respond to Modbus request 43/14, the Service Modbus is added by
 * identification properties<br>
 * 
 * @author Denis Morand
 * 
 */
public class ProxyImporter extends AbstractImporterComponent {
	private static final Logger logger = LoggerFactory.getLogger("protocol.modbus");

	private Factory modbusFactory;
	private Factory modbusFactoryExtended;

	private BundleContext bcontext;

	private RoseMachine roseMachine;

	public ProxyImporter(BundleContext context) {
		this.bcontext = context;
	}

	public List getConfigPrefix() {
		List list = new ArrayList();
		list.add("fr.liglab.adele.modbus.tcp");
		return list;
	}

	protected ServiceRegistration createProxy(EndpointDescription description,
			Map<String, Object> extraProperties) {
		Dictionary props = new Hashtable(description.getProperties());
		ComponentInstance instance;
		try {
			if (props.get("device.identification") != null) {
				instance = modbusFactoryExtended.createComponentInstance(props);
			} else
				instance = modbusFactory.createComponentInstance(props);
			if (instance != null) {
				ServiceRegistration sr = new ModbusDeviceService(instance);
				return sr;
			}
		} catch (Exception ex) {
			logger.error("Proxy creation error" + ex.getStackTrace().toString());
		}

		return null;
	}

	protected void destroyProxy(EndpointDescription description,
			ServiceRegistration registration) {
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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ow2.chameleon.rose.AbstractImporterComponent#stop()
	 */
	protected void stop() {
		super.stop();
	}

	/**
	 * A wrapper for ipojo Component instances
	 */
	class ModbusDeviceService implements ServiceRegistration {

		ComponentInstance instance;

		public ModbusDeviceService(ComponentInstance instance) {
			super();
			this.instance = instance;
			if (logger.isDebugEnabled()) {
				logger.debug("Modbus Proxy instance=" + instance.getInstanceName());
			}

		}

		public ServiceReference getReference() {
			try {
				ServiceReference[] references = instance.getContext()
						.getServiceReferences(instance.getClass().getCanonicalName(),
								"(instance.name=" + instance.getInstanceName() + ")");
				if (references != null)
					return references[0];
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
			instance.dispose();
		}
	}
}
