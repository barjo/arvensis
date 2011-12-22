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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.ow2.chameleon.rose.AbstractExporterComponent;
import org.ow2.chameleon.rose.DynamicExporter;
import org.ow2.chameleon.rose.RoseMachine;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyWebServiceExporter extends AbstractExporterComponent {

	private static final Logger logger = LoggerFactory
			.getLogger("cilia.rose.proxies");
	private final DynamicExporter dynaimp;
	private RoseMachine roseMachine;

	/**
	 * The Proxy wait for a property cilia.export.service =
	 * fr.liglag.adele.ws.server. <br>
	 * This property must be set by the Service OSGi whitch requires to be
	 * exported as Web Service
	 * 
	 * @param context
	 * @throws InvalidSyntaxException
	 */
	public ProxyWebServiceExporter(BundleContext context) throws InvalidSyntaxException {
		super();
		dynaimp = new DynamicExporter.Builder(context,
				"(service.exported=fr.liglab.adele.webservice)").protocol(
				getConfigPrefix()).build();
		logger.debug("Proxy WebServiceExporter Created");
	}

	/** 
	 * List of protocols actually managed
	 */
	public List getConfigPrefix() {
		List list = new ArrayList();
		list.add("org.apache.cxf");
		list.add("cxf");
		list.add("ws");
		list.add("jax-ws");
		return list;
	}

	protected EndpointDescription createEndpoint(ServiceReference sref,
			Map extraProperties) {
		return null;
	}

	protected void destroyEndpoint(EndpointDescription endesc) {
	}

	protected LogService getLogService() {
		return null;
	}

	protected RoseMachine getRoseMachine() {
		return roseMachine;
	}

	protected void start() {
		super.start();
		dynaimp.start();
	}

	protected void stop() {
		super.stop();
		dynaimp.stop();
	}

}
