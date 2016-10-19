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

import org.eclipse.jdt.annotation.NonNull;

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.api.channel.Channel;
import io.openems.impl.protocol.modbus.ModbusElement;

public class UnsignedWordElement extends ModbusElement implements WordElement {
	private final ByteOrder byteOrder;

	public UnsignedWordElement(int address, @NonNull Channel channel, @NonNull ByteOrder byteOrder) {
		super(address, channel);
		this.byteOrder = byteOrder;
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public void setValue(@NonNull Register register) {
		ByteBuffer buff = ByteBuffer.allocate(2).order(byteOrder);
		buff.put(register.toBytes());
		int shortValue = Short.toUnsignedInt(buff.getShort(0));
		setValue(Long.valueOf(shortValue));
	}

	@Override
	public Register toRegister(@NonNull Long value) {
		byte[] b = ByteBuffer.allocate(2).order(byteOrder).putShort(value.shortValue()).array();
		return new SimpleRegister(b[0], b[1]);
	}
}
