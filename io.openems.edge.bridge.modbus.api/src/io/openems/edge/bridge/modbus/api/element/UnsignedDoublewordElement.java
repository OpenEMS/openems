package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.util.Optional;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;

public class UnsignedDoublewordElement extends AbstractDoubleWordElement {
	private WordOrder wordOrder = WordOrder.MSWLSW;

	public UnsignedDoublewordElement(int address) {
		super(address);
	}

	public UnsignedDoublewordElement wordOrder(WordOrder wordOrder) {
		this.wordOrder = wordOrder;
		return this;
	}

	@Override
	protected void _setInputRegisters(InputRegister... registers) {
		// fill buffer
		ByteBuffer buff = ByteBuffer.allocate(4).order(this.getByteOrder());
		if (wordOrder == WordOrder.MSWLSW) {
			buff.put(registers[0].toBytes());
			buff.put(registers[1].toBytes());
		} else {
			buff.put(registers[1].toBytes());
			buff.put(registers[0].toBytes());
		}
		// convert registers to Long
		long value = Integer.toUnsignedLong(buff.getInt(0));
		// set value
		super.setValue(value);
	}

	@Override
	public void _setNextWriteValue(Optional<Long> valueOpt) throws OpenemsException {
		if (valueOpt.isPresent()) {
			byte[] b = ByteBuffer.allocate(4).order(this.getByteOrder()).putInt(valueOpt.get().intValue()).array();
			if (wordOrder == WordOrder.MSWLSW) {
				this.setNextWriteValueRegisters(Optional.of(new Register[] { //
						new SimpleRegister(b[0], b[1]), new SimpleRegister(b[2], b[3]) }));
			} else {
				this.setNextWriteValueRegisters(Optional.of(new Register[] { //
						new SimpleRegister(b[2], b[3]), new SimpleRegister(b[0], b[1]) }));
			}
		} else {
			this.setNextWriteValueRegisters(Optional.empty());
		}
	}
}
