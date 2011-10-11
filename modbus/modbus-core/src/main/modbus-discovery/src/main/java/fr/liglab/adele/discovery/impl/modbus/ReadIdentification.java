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

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decodes Modbus request 43 / 14 (0x2B / 0x0E) <br>
 * Read Device Identification only Basic Device Identification. <br>
 * 
 * see "Modbus Protocol Application V1.1b"
 * 
 * @author Denis Morand
 * 
 */
public class ReadIdentification extends ModbusTCP {

	public static final byte MB_EXCEPTION = (byte) 0x80;

	protected ModbusTCP transport;
	private ModbusFrame request;
	private ModbusFrame response;

	public ReadIdentification(Socket socket) {
		super(socket);
		request = new ModbusFrame();
		response = new ModbusFrame();
	}

	/* MEI Type */
	private final static byte MEI_TYPE = 0x0E;
	/* Code */
	private final static byte IDENTIFICATION = 0x2B;
	/* Read Device ID Code */
	private final static byte BASIC_ID = 0x01;

	/* not implemented */
	private final static byte REGULAR_ID = 0x02;

	/* list of object ID */
	private final static byte OBJ_VENDOR = 0;
	private final static byte OBJ_PRODUCT_CODE = 1;
	private final static byte OBJ_MAJOR_MINOR_REV = 2;
	private final static byte OBJ_VENDOR_URL = 3;
	private final static byte OBJ_PRODUCT_NAME = 4;
	private final static byte OBJ_MODEL_NAME = 5;
	private final static byte OBJ_USER_APP_NAME = 6;

	public Map readModbus4314(int unitID, int transID) {

		Map ident = null;
		boolean fini = false;
		byte nextObject = 0;
		int transactionID;

		transactionID = transID;
		openStreams();

		while (!fini) {
			request.buff[0] = (byte) ((unitID >> 0) & 0xFF);
			request.buff[1] = IDENTIFICATION; /* Function code */
			request.buff[2] = MEI_TYPE; /* MEI type */
			request.buff[3] = BASIC_ID; /* Read Device ID Code */
			request.buff[4] = nextObject; /* Object ID =0 fist sequence */
			request.length = 5;
			request.transID = transactionID++;

			if (!sendFrame(request)) {
				return null;
			}

			if (!receiveFrame(response)) {
				return null;
			}
			if (response.length < 3) {
				return null;
			}

			if (response.transID != transID) {
				return null;
			}

			if (response.buff[0] != ((byte) ((unitID >> 0) & 0xFF))) {
				return null;
			}

			if (response.buff[1] == (IDENTIFICATION & MB_EXCEPTION)) {
				return null;
			}

			if (response.buff[1] != IDENTIFICATION) {
				return null;
			}

			if ((response.buff[2] != MEI_TYPE) || (response.buff[3] != BASIC_ID)) {
				return null;
			}

			/* More follows ? */
			if (response.buff[5] == 0) {
				fini = true; /* no more transaction */
			} else {
				nextObject = response.buff[6]; /* next object to read */
				/* Decoding all objects */
			}
			byte numberOfOject = response.buff[7];
			byte offset = 0;
			if (ident == null)
				ident = new HashMap();

			for (byte cpt = 0; cpt < numberOfOject; cpt++) {
				/* keep object id */
				byte objectId = response.buff[8 + offset];
				/* keep the length */
				byte length = response.buff[9 + offset];

				/* Convert into String */
				char[] str = new char[length];
				for (int i = 0; i < (int) length; i++) {
					str[i] = (char) response.buff[10 + i + offset];
					/* For next objectId */
				}
				offset += length + 2;
				/* Store the ObjectID in the ident */
				switch (objectId) {
				case OBJ_VENDOR:
					/* Store the vendor Name */
					ident.put("vendor.name", String.valueOf(str));
					break;
				case OBJ_PRODUCT_CODE:
					/* Store the product code */
					ident.put("product.code", String.valueOf(str));
					break;
				case OBJ_MAJOR_MINOR_REV:
					/* Store the Major/Minor revision */
					ident.put("major.minor.revision", String.valueOf(str));
					break;
				case OBJ_MODEL_NAME:
					/* Store the model name */
					ident.put("model.name", String.valueOf(str));
					break;
				case OBJ_PRODUCT_NAME:
					/* Store the product code */
					ident.put("product.name", String.valueOf(str));
					break;
				case OBJ_USER_APP_NAME:
					/* Store the product code */
					ident.put("user.application.name", String.valueOf(str));
					break;
				case OBJ_VENDOR_URL:
					/* Store the vendor URL */
					ident.put("vendor.url", String.valueOf(str));
					break;
				default:
				}
			}
		}
		return ident;
	}

	public void close() {
		super.closeStreams();
	}
}
