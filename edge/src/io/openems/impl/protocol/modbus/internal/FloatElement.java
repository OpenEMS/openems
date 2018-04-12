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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.impl.protocol.modbus.ModbusChannel;
import io.openems.impl.protocol.modbus.ModbusElement;

public class FloatElement extends ModbusElement<Long> implements DoublewordElement {
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
	private WordOrder wordOrder = WordOrder.MSWLSW;
	private int multiplier = 0;

	public FloatElement(int address, ModbusChannel<Long> channel) {
		super(address, channel);
	}

	@Override public int getLength() {
		return 2;
	}

	public FloatElement multiplier(int multiplier) {
		this.multiplier = multiplier;
		return this;
	}

	@Override public void setValue(InputRegister register1, InputRegister register2) {
		ByteBuffer buff = ByteBuffer.allocate(4).order(byteOrder);
		if (wordOrder == WordOrder.MSWLSW) {
			buff.put(register1.toBytes());
			buff.put(register2.toBytes());
		} else {
			buff.put(register2.toBytes());
			buff.put(register1.toBytes());
		}
		setValue((long) (buff.order(byteOrder).getFloat(0) * Math.pow(10, multiplier)));
	}

	@Override public Register[] toRegisters(Long value) {
		byte[] b = ByteBuffer.allocate(4).order(byteOrder)
				.putFloat(value.floatValue() / (float) Math.pow(10, multiplier)).array();
		if (wordOrder == WordOrder.MSWLSW) {
			return new Register[] { new SimpleRegister(b[0], b[1]), new SimpleRegister(b[2], b[3]) };
		} else {
			return new Register[] { new SimpleRegister(b[2], b[3]), new SimpleRegister(b[0], b[1]) };
		}
	}
}
