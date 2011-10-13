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

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.liglab.adele.protocol.modbus.ModbusProtocol;
import fr.liglab.adele.protocol.modbus.SlaveException;

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

/**
 * Service Modbus/TCP 
 * @TODO Write functions
 * @author Denis Morand
 *
 */
public class ModbusProxyImpl implements ModbusProtocol {
	protected static final Logger logger = LoggerFactory.getLogger("protocol.modbus");

	private InetAddress m_host;
	protected String m_hostAddress ;
	protected int m_port;

	TCPMasterConnection connection = null; /* the connection */
	ModbusTCPTransaction transaction = null; /* the transaction */

	public ModbusProxyImpl(BundleContext bc) {
		logger.debug("Modbus Service instancied");
	}

	protected void setHost(String host) {
		try {
			m_host = InetAddress.getByName(host);
			m_hostAddress = m_host.getHostAddress();
		} catch (UnknownHostException e) {
			logger.error("Unknown host address " + host);
			m_host = null;
		}
	}

	protected void setPort(int port) {
		m_port = port;
	}

	public void start() {
		if (m_host != null) {
			try {
				connection = new TCPMasterConnection(m_host);
			} catch (Exception e) {
				logger.error("TCP/IP error host=" + m_host.getHostAddress());
				return ;
			}
			connection.setPort(m_port);
			try {
				connection.connect();
				transaction = new ModbusTCPTransaction(connection);
				transaction.setReconnecting(true);

				if (logger.isDebugEnabled()) {
					StringBuffer sb = new StringBuffer("Connection [");
					sb.append(m_host.getHostAddress()).append(":").append(m_port).append("]");
					logger.debug(sb.toString());
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
				transaction.execute();
				regs = ((ReadMultipleRegistersResponse) transaction.getResponse())
						.getRegisters();
				if (logger.isDebugEnabled()) {
					StringBuffer sb = new StringBuffer();
					sb.append("Read Multiple register unitID=").append(unitID);
					sb.append(" ,ref=").append(ref).append(" ,count").append(count);
					logger.debug(sb.toString());
				}
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

	// A supprimer
	public int getUnitID() {
		return 0;
	}
	
}