package io.openems.edge.bridge.modbus.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

public class UnsignedWordElement extends RegisterElement<Integer> {

	private final static ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.BIG_ENDIAN;

	private ByteOrder byteOrder = DEFAULT_BYTE_ORDER;

	public UnsignedWordElement(int address, OnUpdate<Integer> onUpdateCallback) {
		super(address, onUpdateCallback);
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
		ByteBuffer buff = ByteBuffer.allocate(2).order(byteOrder);
		buff.put(registers[0].toBytes());
		int shortValue = Short.toUnsignedInt(buff.getShort(0));
		super.setValue(shortValue);
	}

	// @Override
	// public Register toRegister(Long value) {
	// byte[] b =
	// ByteBuffer.allocate(2).order(byteOrder).putShort(value.shortValue()).array();
	// return new SimpleRegister(b[0], b[1]);
	// }
}
