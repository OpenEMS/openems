package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;

public class UnsignedWordElement extends AbstractWordElement {

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
	protected void _setInputRegisters(InputRegister... registers) {
		// convert registers to Short
		ByteBuffer buff = ByteBuffer.allocate(2).order(byteOrder);
		buff.put(registers[0].toBytes());
		int shortValue = Short.toUnsignedInt(buff.getShort(0));
		// apply scaleFactor
		shortValue = (int) (shortValue * Math.pow(10, this.getScaleFactor()));
		// set value
		super.setValue(shortValue);
	}

	@Override
	public UnsignedWordElement scaleFactor(int scaleFactor) {
		super.scaleFactor(scaleFactor);
		return this;
	}

	@Override
	public UnsignedWordElement priority(Priority priority) {
		return (UnsignedWordElement) super.priority(priority);
	}

	@Override
	public void _setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {
		if (valueOpt.isPresent()) {
			byte[] b = ByteBuffer.allocate(2).order(this.getByteOrder()).putShort(valueOpt.get().shortValue()).array();
			this.setNextWriteValueRegisters(Optional.of(new Register[] { //
					new SimpleRegister(b[0], b[1]) }));
		} else {
			this.setNextWriteValue(Optional.empty());
		}
	}
}
