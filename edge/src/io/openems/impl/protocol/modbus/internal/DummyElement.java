/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.protocol.modbus.internal;

import io.openems.impl.protocol.modbus.ModbusElement;

public class DummyElement extends ModbusElement<Long> {

	private final int length;

	public DummyElement(int address) {
		this(address, address);
	}

	public DummyElement(int fromAddress, int toAddress) {
		super(fromAddress, null);
		this.length = toAddress - fromAddress + 1;
	}

	@Override public int getLength() {
		return length;
	}

	/**
	 * We are not setting a value for a DummyElement.
	 */
	@Override protected void setValue(Long value) {
		return;
	}
}
