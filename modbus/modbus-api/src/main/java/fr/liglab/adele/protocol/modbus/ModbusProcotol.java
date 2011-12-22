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

import java.util.BitSet;
import java.util.Map;

/**
 * 
 * @author Denis Morand
 *
 */
public interface ModbusProcotol {

	/**
	 * Read Holding registers Modbus
	 * 
	 * @param unitID
	 *            : slave ID address
	 * @param ref
	 *            Starting address
	 * @param count
	 *            Quantity of registers
	 * @return Null if Error , array of register value
	 * @throws SlaveException
	 *             ( see Modbus Application protocol V1.1b )
	 */
	public Integer[] getRegisters(int unitID, int ref, int count) throws SlaveException;

	/**
	 * Read Inputs registers Modbus
	 * 
	 * @param unitID
	 *            : slave ID address
	 * @param ref
	 *            Starting address
	 * @param count
	 *            Quantity of registers
	 * @return Null if Error , array of register value
	 * @throws SlaveException
	 *             ( see Modbus Application protocol V1.1b )
	 */
	public Integer[] getInputRegisters(int unitID, int ref, int count)
			throws SlaveException;

	/**
	 * Read Discrete inputs Modbus
	 * 
	 * @param unitID
	 *            : slave ID address
	 * @param ref
	 *            Starting address
	 * @param count
	 *            Quantity of registers
	 * @return Null if Error , bit value
	 * @throws SlaveException
	 *             ( see Modbus Application protocol V1.1b )
	 */
	public BitSet getDiscreteInput(int unitID, int ref, int count) throws SlaveException;

	/**
	 * Read Coils inputs Modbus
	 * 
	 * @param unitID
	 *            : slave ID address
	 * @param ref
	 *            Starting address
	 * @param count
	 *            Quantity of registers
	 * @return Null if Error , bit value
	 * @throws SlaveException
	 *             ( see Modbus Application protocol V1.1b )
	 */
	public BitSet getCoils(int unitID, int ref, int count) throws SlaveException;
		
	/**
	 * return Empty map if no remote identification
	 * @return list of identification parameters decoded from request 43/14 
	 */
	public Map getIdentification() ;
	
	/** 
	 * Return for Debug purpose only !
	 */
	public Map getDebugInfo() ;
}
