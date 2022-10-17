// CHECKSTYLE:OFF
/*
*   EDCOM 8.1 is a java cross platform library for communication with 10kW
*   hybrid Inverter (Katek Memmingen GmbH).
*   Copyright (C) 2022 Katek Memmingen GmbH
*
*   This program is free software: you can redistribute it and/or modify
*   it under the terms of the GNU Lesser General Public License as published by
*   the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*   
*   You should have received a copy of the GNU Lesser General Public License
*   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.ed.edcom;

/**
 * Binary data representation.
 */
public final class DspBlob {

	byte[] bytes;
	byte[] acknowledge;
	long timeOut;
	int acknowledgeStatus;
	byte messageType;
	int retryCount;
	long ts;

	/**
	 * Constructor
	 *
	 * @param bytes       bytes to send
	 * @param acknowledge required acknowledge bytes
	 * @param timeOut     timeout in millisecond
	 * @param messageType message type
	 * @param retryCount  retry count
	 * @throws java.lang.Exception wrong parameters
	 */
	public DspBlob(byte bytes[], byte[] acknowledge, long timeOut, byte messageType, int retryCount) throws Exception {
		if (Util.userId < 1) {
			throw new Exception("Library initialization error");
		}
		this.bytes = bytes.clone();
		this.timeOut = timeOut;
		this.messageType = messageType;
		if (acknowledge != null) {
			this.acknowledge = acknowledge.clone();
		} else {
			this.acknowledge = null;
		}
		this.retryCount = retryCount;
	}

	/**
	 * Get acknowledge status
	 *
	 * @return '1' acknowledge received '0' wait '-1' no or bad acknowledge received
	 */
	public int getAcknowledgeStatus() {
		return acknowledgeStatus;
	}

	byte getType() {
		return messageType;
	}
}
//CHECKSTYLE:ON
