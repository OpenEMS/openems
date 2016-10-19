/*
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH & Co. KG
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

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.openems.impl.protocol.modbus.internal;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.api.channel.Channel;
import io.openems.impl.protocol.modbus.ModbusElement;

public class SignedDoublewordElement extends ModbusElement implements DoublewordElement {
	private final ByteOrder byteOrder;
	private final WordOrder wordOrder;

	public SignedDoublewordElement(int address, @NonNull Channel channel, @NonNull ByteOrder byteOrder,
			@NonNull WordOrder wordOrder) {
		super(address, channel);
		this.byteOrder = byteOrder;
		this.wordOrder = wordOrder;
	}

	@Override
	public int getLength() {
		return 2;
	}

	@Override
	public void setValue(@NonNull Register register1, @NonNull Register register2) {
		ByteBuffer buff = ByteBuffer.allocate(4).order(byteOrder);
		if (wordOrder == WordOrder.MSWLSW) {
			buff.put(register1.toBytes());
			buff.put(register2.toBytes());
		} else {
			buff.put(register2.toBytes());
			buff.put(register1.toBytes());
		}
		setValue(BigInteger.valueOf(buff.order(byteOrder).getInt(0)));
	}

	@Override
	public Register[] toRegisters(@NonNull BigInteger value) {
		byte[] b = ByteBuffer.allocate(4).order(byteOrder).putInt(value.intValue()).array();
		if (wordOrder == WordOrder.MSWLSW) {
			return new Register[] { new SimpleRegister(b[0], b[1]), new SimpleRegister(b[2], b[3]) };
		} else {
			return new Register[] { new SimpleRegister(b[2], b[3]), new SimpleRegister(b[0], b[1]) };
		}
	}
}
