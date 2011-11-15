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

import java.io.*;
import java.net.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal layer Transport Modbus/TCP 
 * 
 * @author Denis Morand
 * 
 */
public class ModbusTCP {
	private static final Logger logger = LoggerFactory.getLogger("modbus.discovery");

	public static final int MBAP_LENGTH = 6;
	public static final int LENGTH_MODBUS_FRAME = 255;
	public static final int MAX_TRANSACTION_LENGTH = MBAP_LENGTH + LENGTH_MODBUS_FRAME;
	public static final short MB_PROTOCOL_ID = 0;

	private Socket socket ;
	private BufferedInputStream in;
	private BufferedOutputStream out;

	private byte[] received_mbap = new byte[MBAP_LENGTH];
	private byte[] sent_mbap = new byte[MBAP_LENGTH];

	private int count = 0;
	private int recv = 0;

	private int dataLength;

	public ModbusTCP(Socket socket) {
		try {
			this.socket = socket ;
			socket.setTcpNoDelay(true);
		} catch (SocketException e) {
			logger.error("socket Error");
		}		
	}
	
	public boolean sendFrame(ModbusFrame msg) {
		sent_mbap[0] = (byte) ((msg.transID >> 8) & 0xFF);
		sent_mbap[1] = (byte) (msg.transID & 0xFF);

		sent_mbap[2] = (byte) ((MB_PROTOCOL_ID >> 8) & 0xFF);
		sent_mbap[3] = (byte) (MB_PROTOCOL_ID & 0xFF);

		sent_mbap[4] = (byte) 0x00;
		sent_mbap[5] = (byte) (msg.length & 0xFF);

		try {
			out.write(sent_mbap, 0, MBAP_LENGTH);
			out.write(msg.buff, 0, msg.length);
			out.flush();

		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	public boolean receiveFrame(ModbusFrame msg) {
		count = 0;
		while (count < MBAP_LENGTH) {
			try {
				recv = in.read(received_mbap, count, MBAP_LENGTH - count);
			} catch (IOException ex) {
				return false;
			}
			if (recv == -1) {
				return false;
			}
			count += recv;
		}

		msg.transID = (int) ((received_mbap[0] << 8) + received_mbap[1]);

		if ((short) ((received_mbap[2] << 8) + received_mbap[3]) != MB_PROTOCOL_ID) {
			return false;
		}

		if (received_mbap[4] != (byte) 0x00) {
			return false;
		}

		if (received_mbap[5] == (byte) 0x00 || received_mbap[5] == (byte) 0x01) {
			return false;
		}

		dataLength = (int) received_mbap[5];
		count = 0;
		while (count < dataLength) {
			try {
				recv = in.read(msg.buff, count, dataLength - count);
			} catch (IOException ex) {
				logger.error("Error read TCP/IP ");
				return false;
			}
			if (recv == -1) {
				logger.error("Error read TCP/IP ");
				return false;
			}
			count += recv;
		}
		msg.length = dataLength;
		return true;
	}

	protected void openStreams() {
		try {
			out = new BufferedOutputStream(socket.getOutputStream());
			in = new BufferedInputStream(socket.getInputStream());
		} catch (IOException ex) {
			logger.error("Input/Outstream socket error");
			return;
		}		
	}
	protected void closeStreams() {
		try {
			in.close();
			out.close();
		} catch (Exception e) {
			logger.error("Input/Outstream close socket error");
		}
	}
}
