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

package fr.liglab.adele.discovery.impl.modbus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
 * Periodic scan devices between 2 IP V4:port <br>
 * 
 * @author Denis Morand
 * 
 */
public class ModbusTCPScanner extends TimerTask  {
	private static final Logger logger = LoggerFactory.getLogger("discovery.modbus");

	private RoseMachine roseMachine;
	private RemoteServiceAdmin adminService ;
	private InetAddress startAddress, endAddress;
	private int m_delay, m_period, m_timeout, m_port;
	private String m_domainID ;

	private Timer timer;
	public ModbusTCPScanner(BundleContext bc) {
		timer = new Timer() ;
	}

	public void setStartAddress(String addr) {
		try {
			startAddress = InetAddress.getByName(addr);
		} catch (UnknownHostException e) {
			String str = "Properties 'start.address' is not a valid address" + addr;
			logger.error(str);
			throw new IllegalArgumentException(str);
		}
	}

	public void setEndAddress(String addr) { 
		try {
			endAddress = InetAddress.getByName(addr);
		} catch (UnknownHostException e) {
			String str = "Properties 'end.address' is not a valid address" + addr;
			logger.error(str);
			throw new IllegalArgumentException(str);
		}
	}

	public void runScan() {
		checksParam();
		timer.scheduleAtFixedRate(this, m_delay, m_period);

		if (logger.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer("Scanner modbus started [from ");
			sb.append(startAddress.toString()).append(" to ");
			sb.append(endAddress.toString());
			sb.append(", domain :").append(m_domainID).append("]");
			logger.debug(sb.toString());
		}
	}

	public void cancelScan() {
		timer.cancel();
	}

	private void setRemoteDevice(Socket socket) {
		/* Generate the unique device ID */
		String deviceID = generateID(socket.getInetAddress().getHostAddress(),
				socket.getPort());
		/* Checks if that ID is already in registry */
		if (!isEndPointRegistered(deviceID)) {
			Map deviceProperties = setDeviceEndPoint(socket.getInetAddress()
					.getHostAddress(), socket.getPort());
			if (socket != null) {
				Map param = readModbusIdent(socket);
				if (param != null) {
					deviceProperties.put("device.identification", param) ;
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Device Present at " + deviceID);
				logger.debug("Device info :" + deviceProperties.toString());
			}
			roseMachine.putRemote(deviceID, new EndpointDescription(deviceProperties));
		} else {
			logger.trace("Device already registered "
					+ socket.getRemoteSocketAddress().toString());
		}
	}

	private void resetRemoteDevice(InetAddress addr, int port) {
		EndpointDescription epd = roseMachine.removeRemote(generateID(addr
				.getHostAddress().toString(), port));
		if (epd != null) {
			logger.debug("Device removed:" + epd.getProperties().toString());
		}
	}

	private Socket tcpConnect(InetAddress addr, int port) {
		Socket socket = null;
		try {
			socket = new Socket(addr, port);
		} catch (Exception e) {
		}
		return socket;
	}

	private Map readModbusIdent(Socket socket) {
		ReadIdentification modbus;
		Map param = null;
		if (socket != null) {
			try {
				modbus = new ReadIdentification(socket);
				/* Check unitID=0 first , then unitID=255 */
				param = modbus.readModbus4314(0, 0xA1);
				if (param == null)
					param = modbus.readModbus4314(255, 0x2B);
				modbus.close();
			} catch (Exception e) {
				logger.error("Modbus Read identification error");
				e.printStackTrace();
			}
		}
		return param;
	}

	/**
	 * 
	 * Checks Modbus Device over IP
	 * 
	 */
	public void run() {
		InetAddress address;
		boolean isDone = false;
		Socket socket;

		IPAddressNexter nexter = new IPAddressNexter(startAddress, endAddress);
		address = startAddress;
		do {
			if (ping(address, m_timeout)) {
				logger.trace("Device present at :"+address.getHostAddress());
				socket = tcpConnect(address, m_port);
				if (socket != null) {
					setRemoteDevice(socket);
					try {
						socket.close();
					} catch (IOException e) {
					}
				}

			} else {
				logger.trace("Device absent at :"+address.getHostAddress());
				resetRemoteDevice(address, m_port);
			}

			if (nexter.hasMoreElements()) {
				address = (InetAddress) nexter.nextElement();
			} else
				isDone = true;
		} while (isDone == false);
	}

	private static boolean ping(InetAddress addr, int to) {
		boolean pong = false;
		try {
			pong = InetAddress.getByName(addr.getHostName().toString()).isReachable(to);
		} catch (IOException e) {
		}
		return pong;
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
			};
		}
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map setDeviceEndPoint(String hostAddr, int port) {
		Map m_props = new HashMap();
		m_props.put(RemoteConstants.ENDPOINT_ID, generateID(hostAddr, port));
		m_props.put(Constants.OBJECTCLASS, new String[] { "NoneObject" });
		m_props.put(RemoteConstants.SERVICE_IMPORTED, "fr.liglab.adele.modbus.tcp");
		m_props.put(RemoteConstants.SERVICE_IMPORTED_CONFIGS, "");
		/* Parameter for the Service */
		m_props.put("device.type", "modbus.slave");
		m_props.put("device.ip.address", hostAddr);
		m_props.put("device.ip.port", port);
		m_props.put("domain.id", m_domainID);
		return m_props;
	}

	private static String generateID(String addr, int port) {
		StringBuffer sb = new StringBuffer(addr);
		sb.append(":").append(Integer.toString(port));
		return sb.toString();
	}

	private void checksParam() {
		
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
		if (m_timeout <= 0) {
			String str = "Properties 'ping.time.out' must not be less than 0" + m_timeout;
			logger.error(str);
			throw new IllegalArgumentException(str);
		}
		if (m_domainID ==null) {
			/* generate a "unique"  domain name */
			m_domainID = "domain.ip-"+
			              startAddress.toString()+"-"+
			              endAddress.toString()+"-"+System.currentTimeMillis();
		}
	}

}
