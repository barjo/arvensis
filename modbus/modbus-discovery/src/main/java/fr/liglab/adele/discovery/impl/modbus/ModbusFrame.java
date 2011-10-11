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

/**
 * Modbus Frame <br>
 * data = 256 byte length maximal <br>
 * 1 byte length ( data length) <br>
 * 1 byte transaction identifier <br>
 * 
 * @author Denis Morand
 */
public class ModbusFrame {

	public byte[] buff;
	public int length;
	public int transID;

	public ModbusFrame() {
		buff = new byte[256];
		length = 0;
		transID = 0;
	}
}
