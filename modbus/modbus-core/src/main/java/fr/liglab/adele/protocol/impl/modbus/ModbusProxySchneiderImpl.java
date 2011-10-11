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

import java.util.Collections;
import java.util.Map;

import org.osgi.framework.BundleContext;

import fr.liglab.adele.protocol.modbus.ModbusProcotolSchneider;

/**
 * Extension Service Modbus <br>
 * Provide identification and additional information relative to the physical <br>
 * and functional description of a remote device.
 * 
 * @author Denis Morand
 * 
 */
public class ModbusProxySchneiderImpl extends ModbusProxyImpl implements
		ModbusProcotolSchneider {

	private Map m_identification;

	public ModbusProxySchneiderImpl(BundleContext bc) {
		super(bc);
		m_identification = Collections.EMPTY_MAP;
	}

	protected void setHost(String host) {
		super.setHost(host);
	}

	protected void setPort(int port) {
		super.setPort(port);
	}

	protected void setModbusID(int id) {
		super.setModbusID(id);
	}

	protected void setIdentification(Map ident) {
		if (ident != null)
			m_identification = Collections.unmodifiableMap(ident);
	}

	public Map getIdentification() {
		return m_identification;
	}

}
