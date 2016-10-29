/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
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

import java.util.Optional;

import io.openems.api.channel.numeric.NumericChannel;
import io.openems.impl.protocol.modbus.ModbusElement;

public class DummyElement extends ModbusElement {

	private final int length;

	public DummyElement(int address, int length) {
		super(address, new NumericChannel(Optional.empty(), null, "", null, null, null, null, null));
		this.length = length;
	}

	@Override
	public int getLength() {
		return length;
	}

	/**
	 * We are not setting a value for a DummyElement.
	 */
	@Override
	protected void setValue(Long value) {
		return;
	}
}
