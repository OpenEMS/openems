package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

public class SignedWordElement extends AbstractWordElement {

	public SignedWordElement(int address) {
		super(address);
	}

	@Override
	protected void _setInputRegisters(InputRegister... registers) {
		// convert registers to Short
		ByteBuffer buff = ByteBuffer.allocate(2).order(getByteOrder());
		buff.put(registers[0].toBytes());
		int shortValue = buff.order(getByteOrder()).getShort(0);
		// apply scaleFactor
		shortValue = (int) (shortValue * Math.pow(10, this.getScaleFactor()));
		// set value
		super.setValue(shortValue);
	}

	@Override
	public SignedWordElement scaleFactor(int scaleFactor) {
		return (SignedWordElement) super.scaleFactor(scaleFactor);
	}

	// @Override
	// public Register toRegister(Long value) {
	// byte[] b =
	// ByteBuffer.allocate(2).order(byteOrder).putShort(value.shortValue()).array();
	// return new SimpleRegister(b[0], b[1]);
	// }
}
