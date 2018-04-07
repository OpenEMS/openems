package io.openems.edge.bridge.modbus.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

public class SignedWordElement extends RegisterElement<Integer> {
	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	public SignedWordElement(int address) {
		super(address);
	}

	public SignedWordElement byteOrder(ByteOrder byteOrder) {
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
		int shortValue = buff.order(byteOrder).getShort(0);
		// apply scaleFactor
		shortValue = (int) (shortValue * Math.pow(10, this.scaleFactor));
		// set value
		super.setValue(shortValue);
	}

	private int scaleFactor = 0;

	@Override
	public SignedWordElement scaleFactor(int scaleFactor) {
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
