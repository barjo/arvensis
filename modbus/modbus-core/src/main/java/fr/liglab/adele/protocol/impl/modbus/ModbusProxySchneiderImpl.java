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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.Collections;
import java.util.Map;

import net.wimpi.modbus.ModbusException;
import net.wimpi.modbus.ModbusIOException;
import net.wimpi.modbus.ModbusSlaveException;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.ReadCoilsRequest;
import net.wimpi.modbus.msg.ReadCoilsResponse;
import net.wimpi.modbus.msg.ReadInputDiscretesRequest;
import net.wimpi.modbus.msg.ReadInputDiscretesResponse;
import net.wimpi.modbus.msg.ReadInputRegistersRequest;
import net.wimpi.modbus.msg.ReadInputRegistersResponse;
import net.wimpi.modbus.msg.ReadMultipleRegistersRequest;
import net.wimpi.modbus.msg.ReadMultipleRegistersResponse;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.procimg.InputRegister;
import net.wimpi.modbus.procimg.Register;
import net.wimpi.modbus.util.BitVector;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.liglab.adele.protocol.modbus.ModbusProcotolSchneider;
import fr.liglab.adele.protocol.modbus.SlaveException;

/**
 * Extension Service Modbus <br>
 * Provide identification and additional information relative to the physical <br>
 * and functional description of a remote device.
 * 
 * @author Denis Morand
 * 
 */
public class ModbusProxySchneiderImpl implements ModbusProcotolSchneider {

	private Map m_identification;

	private static final Logger logger = LoggerFactory.getLogger("modbus.protocol");

	private InetAddress m_host;
	protected String m_hostAddress;
	protected int m_port;

	private TCPMasterConnection connection = null; /* the connection */
	private ModbusTCPTransaction transaction = null; /* the transaction */

	public ModbusProxySchneiderImpl(BundleContext bc) {
		m_identification = Collections.EMPTY_MAP;
	}

	private void setIdentification(Map ident) {
		if (ident != null)
			m_identification = Collections.unmodifiableMap(ident);
	}

	public Map getIdentification() {
		return m_identification;
	}

	private void setHost(String host) {
		try {
			m_host = InetAddress.getByName(host);
			m_hostAddress = m_host.getHostAddress();
		} catch (UnknownHostException e) {
			logger.error("Unknown host address " + host);
			m_host = null;
		}
	}

	private void setPort(int port) {
		m_port = port;
	}

	public void start() {
		if (m_host != null) {
			try {
				connection = new TCPMasterConnection(m_host);
			} catch (Exception e) {
				logger.error("TCP/IP error host=" + m_host.getHostAddress());
				return;
			}
			connection.setPort(m_port);
			try {
				connection.connect();
				transaction = new ModbusTCPTransaction(connection);
				transaction.setReconnecting(true);

				if (logger.isInfoEnabled()) {
					StringBuffer sb = new StringBuffer("Connection [");
					sb.append(m_host.getHostAddress()).append(":").append(m_port)
							.append("]");
					logger.info(sb.toString());
				}
			} catch (Exception e) {
				/* Should never append */
				logger.error("TCP/IP connection failure =" + m_host.getHostAddress());
			}
		}
	}

	public void stop() {
		if (m_host != null) {
			if (connection != null) {
				connection.close();
			}
		}
	}

	public Integer[] getRegisters(int unitID, int ref, int count) throws SlaveException {
		Register[] regs = null;
		Integer[] integer = null;
		if (connection != null) {
			ReadMultipleRegistersRequest request = null;
			request = new ReadMultipleRegistersRequest(ref, count);
			request.setUnitID(unitID);
			/* Request processing */
			transaction.setRequest(request);
			try {
				if (logger.isDebugEnabled()) {
					StringBuffer sb = new StringBuffer();
					sb.append("Read Multiple registers, unitID=").append(unitID);
					sb.append(" ,ref=").append(ref).append(" ,count").append(count);
					logger.debug(sb.toString());
				}
				transaction.execute();
				regs = ((ReadMultipleRegistersResponse) transaction.getResponse())
						.getRegisters();
				if (regs != null) {
					integer = new Integer[regs.length];
					for (int i = 0; i < integer.length; i++) {
						integer[i] = new Integer(regs[i].getValue());
					}
				}
			} catch (ModbusIOException e) {
				logger.error("Modbus IO Error :", e.getMessage());
			} catch (ModbusSlaveException e) {
				throw new SlaveException(e.getType());
			} catch (ModbusException e) {
				logger.error("Modbus Exception :", e.getMessage());
			}
		}
		return integer;
	}

	public Integer[] getInputRegisters(int unitID, int ref, int count)
			throws SlaveException {
		InputRegister[] regs = null;
		Integer[] integer = null;

		if (connection != null) {
			ReadInputRegistersRequest request = null;
			ReadInputRegistersResponse response = null;
			request = new ReadInputRegistersRequest(ref, count);
			request.setUnitID(unitID);
			try {
				if (logger.isDebugEnabled()) {
					StringBuffer sb = new StringBuffer();
					sb.append("Read Input registers, unitID=").append(unitID);
					sb.append(" ,ref=").append(ref).append(" ,count").append(count);
					logger.debug(sb.toString());
				}
				transaction.execute();
				regs = ((ReadInputRegistersResponse) transaction.getResponse())
						.getRegisters();
				if (regs != null) {
					integer = new Integer[regs.length];
					for (int i = 0; i < integer.length; i++) {
						integer[i] = new Integer(regs[i].getValue());
					}
				}

			} catch (ModbusIOException e) {
				logger.error("Modbus IO Error :", e.getMessage());
			} catch (ModbusSlaveException e) {
				throw new SlaveException(e.getType());
			} catch (ModbusException e) {
				logger.error("Modbus Exception :", e.getMessage());
			}
		}
		return integer;
	}

	public BitSet getDiscreteInput(int unitID, int ref, int count) throws SlaveException {
		BitVector regs = null;
		BitSet bits = null;

		if (connection != null) {
			ReadInputDiscretesRequest request = null;
			request = new ReadInputDiscretesRequest(ref, count);
			request.setUnitID(unitID);
			/* Request processing */
			transaction.setRequest(request);
			try {
				if (logger.isDebugEnabled()) {
					StringBuffer sb = new StringBuffer();
					sb.append("Read Discrete Inputs, unitID=").append(unitID);
					sb.append(" ,ref=").append(ref).append(" ,count").append(count);
					logger.debug(sb.toString());
				}
				transaction.execute();
				regs = ((ReadInputDiscretesResponse) transaction.getResponse())
						.getDiscretes();
				if (regs != null) {
					bits = new BitSet(regs.size());
					for (int i = 0; i < regs.size(); i++) {
						bits.set(i, regs.getBit(i));
					}
				}
			} catch (ModbusIOException e) {
				logger.error("Modbus IO Error :", e.getMessage());
			} catch (ModbusSlaveException e) {
				throw new SlaveException(e.getType());
			} catch (ModbusException e) {
				logger.error("Modbus Exception :", e.getMessage());
			}
		}
		return bits;
	}

	public BitSet getCoils(int unitID, int ref, int count) throws SlaveException {
		BitVector regs = null;
		BitSet bits = null;

		if (connection != null) {
			ReadCoilsRequest request = null;
			request = new ReadCoilsRequest(ref, count);
			request.setUnitID(unitID);
			/* Request processing */
			transaction.setRequest(request);
			try {
				if (logger.isDebugEnabled()) {
					StringBuffer sb = new StringBuffer();
					sb.append("Read Coils, unitID=").append(unitID);
					sb.append(" ,ref=").append(ref).append(" ,count").append(count);
					logger.debug(sb.toString());
				}
				transaction.execute();
				regs = ((ReadCoilsResponse) transaction.getResponse()).getCoils();
				if (regs != null) {
					bits = new BitSet(regs.size());
					for (int i = 0; i < regs.size(); i++) {
						bits.set(i, regs.getBit(i));
					}
				}
			} catch (ModbusIOException e) {
				logger.error("Modbus IO Error :", e.getMessage());
			} catch (ModbusSlaveException e) {
				throw new SlaveException(e.getType());
			} catch (ModbusException e) {
				logger.error("Modbus Exception :", e.getMessage());
			}
		}
		return bits;
	}

}
