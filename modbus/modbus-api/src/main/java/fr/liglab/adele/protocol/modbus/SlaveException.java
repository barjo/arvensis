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

package fr.liglab.adele.protocol.modbus;

/**
 * Class that implements a <tt>ModbusException</tt>.
 * see Modbus Application protocol V1.1b 
 * 
 * @author Denis Morand
 */
public class SlaveException extends ModbusException {
	private static final long serialVersionUID = -1302115889774869425L;
	private int m_mbException;

	public SlaveException(int modbusException) {
		super();
		m_mbException = modbusException;
	}

	public int getExceptionNumber() {
		return m_mbException;
	}

	public String getMessage() {
		return "Modbus Exception = " + m_mbException;
	}
}
