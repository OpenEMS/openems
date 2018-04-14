package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

public class UnsignedDoublewordElement extends AbstractDoubleWordElement {
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
	private WordOrder wordOrder = WordOrder.MSWLSW;

	public UnsignedDoublewordElement(int address) {
		super(address);
	}

	public UnsignedDoublewordElement byteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		return this;
	}

	public UnsignedDoublewordElement wordOrder(WordOrder wordOrder) {
		this.wordOrder = wordOrder;
		return this;
	}

	@Override
	protected void _setInputRegisters(InputRegister... registers) {
		// fill buffer
		ByteBuffer buff = ByteBuffer.allocate(4).order(byteOrder);
		if (wordOrder == WordOrder.MSWLSW) {
			buff.put(registers[0].toBytes());
			buff.put(registers[1].toBytes());
		} else {
			buff.put(registers[1].toBytes());
			buff.put(registers[0].toBytes());
		}
		// convert registers to Long
		long value = Integer.toUnsignedLong(buff.getInt(0));
		// apply scaleFactor
		value = (int) (value * Math.pow(10, this.getScaleFactor()));
		// set value
		super.setValue(value);
	}

	@Override
	public UnsignedDoublewordElement scaleFactor(int scaleFactor) {
		super.scaleFactor(scaleFactor);
		return this;
	}

	// @Override
	// public Register[] toRegisters(Long value) {
	// byte[] b =
	// ByteBuffer.allocate(4).order(byteOrder).putInt(value.intValue()).array();
	// if (wordOrder == WordOrder.MSWLSW) {
	// return new Register[] { new SimpleRegister(b[0], b[1]), new
	// SimpleRegister(b[2], b[3]) };
	// } else {
	// return new Register[] { new SimpleRegister(b[2], b[3]), new
	// SimpleRegister(b[0], b[1]) };
	// }
	// }
}
