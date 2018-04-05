package io.openems.edge.bridge.modbus.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

public class UnsignedWordElement extends RegisterElement<Integer> {

	private final static ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.BIG_ENDIAN;

	private ByteOrder byteOrder = DEFAULT_BYTE_ORDER;

	public UnsignedWordElement(int address) {
		super(address);
	}

	public UnsignedWordElement byteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
		return this;
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	protected void _setInputRegisters(InputRegister... registers) {
		// convert registers to Short
		ByteBuffer buff = ByteBuffer.allocate(2).order(byteOrder);
		buff.put(registers[0].toBytes());
		int shortValue = Short.toUnsignedInt(buff.getShort(0));
		// apply scaleFactor
		shortValue = (int) (shortValue * Math.pow(10, this.scaleFactor));
		// set value
		super.setValue(shortValue);
	}

	private int scaleFactor = 0;

	@Override
	public UnsignedWordElement scaleFactor(int scaleFactor) {
		this.scaleFactor = scaleFactor;
		return this;
	}

	// @Override
	// public Register toRegister(Long value) {
	// byte[] b =
	// ByteBuffer.allocate(2).order(byteOrder).putShort(value.shortValue()).array();
	// return new SimpleRegister(b[0], b[1]);
	// }
}
