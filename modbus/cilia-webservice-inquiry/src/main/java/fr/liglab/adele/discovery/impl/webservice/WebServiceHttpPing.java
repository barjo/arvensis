/* Copyright Adele Team LIG
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

package fr.liglab.adele.discovery.impl.webservice;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ImportReference;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.ow2.chameleon.rose.RoseMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple http ping url
 * 
 * @author Denis Morand
 * 
 */
public class WebServiceHttpPing extends TimerTask {
	private static final Logger logger = LoggerFactory.getLogger("webservice.inquiry");
	private BundleContext m_bundleContext;
	private int m_delay, m_period;
	private List m_listURL;
	private Properties m_wsRankingProperties;
	private String m_urlRanking;
	private String m_urlWebService;
	private Timer m_timer;
	private RoseMachine roseMachine;
	private RemoteServiceAdmin adminService;
	private String m_factoryName ;
	private URL m_url;

	public WebServiceHttpPing(BundleContext context) {
		m_bundleContext = context;
		m_wsRankingProperties = new Properties();
		m_timer = new Timer();
	}

	private static final boolean pingHttp(URL url ) {
		boolean isAvailable = false;
		try {
			final HttpURLConnection connection = (HttpURLConnection)url.openConnection() ;
			connection.connect();
			connection.disconnect();
			isAvailable = true;
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return isAvailable;
	}

	public void runScan() {
		checksParam();
		m_timer.scheduleAtFixedRate(this, m_delay, m_period);
		if (logger.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer("Scanner Web Service started");
			logger.debug(sb.toString());
		}
	}

	public void cancelScan() {
		m_timer.cancel();
	}

	@Override
	public void run() {
		if (pingHttp(m_url)) {
			logger.debug("Url "+m_urlWebService+" reachable" );
			setRemoteWebService(m_urlWebService);
		} else {
			logger.debug("Url "+m_urlWebService+" not reachable" );
			resetRemoteWebService(m_urlWebService);
		}
	}

	private void checksParam() {
		if (m_urlWebService ==null) {
			String str = "URL must not be null" ;
			logger.error(str);
			throw new IllegalArgumentException(str);			
		}
		try {
			m_url= new URL(m_urlWebService);
		} catch (MalformedURLException e) {
			String str= "Malformed url :"+m_urlWebService;
			logger.error(str);
			throw new IllegalArgumentException(str);
		}
		
		if (m_delay <= 0) {
			String str = "Properties 'scan.delay' must not be a negative value" + m_delay;
			logger.error(str);
			throw new IllegalArgumentException(str);
		}
		if (m_period <= 0) {
			String str = "Properties 'scan.period' must not be a negative value"
					+ m_period;
			logger.error(str);
			throw new IllegalArgumentException(str);
		}

		if (m_urlRanking != null) {
			try {
				m_wsRankingProperties.load(new URL(m_urlRanking).openStream());
				logger.debug("Properties read " + m_wsRankingProperties.toString());
			} catch (MalformedURLException e) {
				logger.error("Invalid URL");
			} catch (IOException e) {
				logger.error("file " + m_urlRanking + " not existing");
			}
		}
	}

	private boolean isEndPointRegistered(String id) {
		Collection registry = adminService.getImportedEndpoints();
		if (registry != null) {
			ImportReference reference;
			EndpointDescription epd;
			Iterator it = registry.iterator();
			while (it.hasNext()) {
				reference = ((ImportReference) it.next());
				epd = reference.getImportedEndpoint();
				if (epd.getId().equals(id)) {
					return true;
				}
			}
		}
		return false;
	}

	private void setRemoteWebService(String url) {
		Map props;
		if (!isEndPointRegistered(url)) {
			props = new HashMap();
			props.put(RemoteConstants.ENDPOINT_ID, url);
			props.put(Constants.OBJECTCLASS, new String[] { "None" });
			props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "None");
			props.put(RemoteConstants.SERVICE_IMPORTED, "fr.liglab.adele.webservice");
			props.put("service.factory", m_factoryName);
			try {
				roseMachine.putRemote(url, new EndpointDescription(props));
			}
			catch (Exception e) {
				roseMachine.removeRemote(url);
				System.out.println("REMOVED end point ");
			}
		}
	}

	private void resetRemoteWebService(String url) {
		EndpointDescription epd = roseMachine.removeRemote(url);
		if (epd != null) {
			logger.debug("Web Service disappear:" + epd.getProperties().toString());
		}
	}

}