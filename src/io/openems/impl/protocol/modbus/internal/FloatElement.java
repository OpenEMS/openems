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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.api.channel.NumericChannel;
import io.openems.impl.protocol.modbus.ModbusElement;

public class FloatElement extends ModbusElement implements DoublewordElement {
	private final ByteOrder byteOrder;
	private final WordOrder wordOrder;

	public FloatElement(int address, NumericChannel channel, ByteOrder byteOrder, WordOrder wordOrder) {
		super(address, channel);
		this.byteOrder = byteOrder;
		this.wordOrder = wordOrder;
	}

	@Override
	public int getLength() {
		return 2;
	}

	@Override
	public void setValue(Register register1, Register register2) {
		ByteBuffer buff = ByteBuffer.allocate(4).order(byteOrder);
		if (wordOrder == WordOrder.MSWLSW) {
			buff.put(register1.toBytes());
			buff.put(register2.toBytes());
		} else {
			buff.put(register2.toBytes());
			buff.put(register1.toBytes());
		}
		setValue((long) buff.order(byteOrder).getFloat(0));
	}

	@Override
	public Register[] toRegisters(Long value) {
		byte[] b = ByteBuffer.allocate(4).order(byteOrder).putFloat(value.floatValue()).array();
		if (wordOrder == WordOrder.MSWLSW) {
			return new Register[] { new SimpleRegister(b[0], b[1]), new SimpleRegister(b[2], b[3]) };
		} else {
			return new Register[] { new SimpleRegister(b[2], b[3]), new SimpleRegister(b[0], b[1]) };
		}
	}
}
