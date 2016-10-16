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

import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.api.channel.Channel;

public class UnsignedWordElement extends Element implements WordElement {
	final ByteOrder byteOrder;

	public UnsignedWordElement(int address, Channel channel, int multiplier, int delta, ByteOrder byteOrder) {
		super(address, channel, multiplier, delta);
		this.byteOrder = byteOrder;
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public BigInteger getMaxValue() {
		return BigInteger.valueOf(Short.MAX_VALUE - Short.MIN_VALUE);
	}

	@Override
	public BigInteger getMinValue() {
		return BigInteger.valueOf(0);
	}

	@Override
	public void setValue(Register register) {
		ByteBuffer buff = ByteBuffer.allocate(2).order(byteOrder);
		buff.put(register.toBytes());
		int shortValue = Short.toUnsignedInt(buff.getShort(0));
		setValue(BigInteger.valueOf(shortValue).multiply(multiplier).subtract(delta));
	}

	public Register toRegister(BigInteger value) {
		byte[] b = ByteBuffer.allocate(2).order(byteOrder).putShort(value.add(delta).divide(multiplier).shortValue())
				.array();
		return new SimpleRegister(b[0], b[1]);
	}
}
