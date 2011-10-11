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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Simple IP V4 address nexter
 * 
 * @author Denis Morand
 * 
 */
public class IPAddressNexter implements Enumeration {
	private InetAddress currentAddress, boundMaxAddress;

	public IPAddressNexter(InetAddress start, InetAddress end) {
		currentAddress = start;
		boundMaxAddress = end;
	}

	public boolean hasMoreElements() {
		synchronized (currentAddress) {
			return !Arrays.equals(currentAddress.getAddress(),
					boundMaxAddress.getAddress());
		}
	}

	public Object nextElement() {
		synchronized (currentAddress) {
			byte[] address = currentAddress.getAddress();
			address[3]++;
			for (int i = 3; i > 0; i--) {
				if (address[i] == 256) {
					address[i] = 0;
					address[i - 1]++;
				}
			}
			try {
				currentAddress = InetAddress.getByAddress(address);
			} catch (UnknownHostException e) {
			}
			return currentAddress;
		}
	}
}
