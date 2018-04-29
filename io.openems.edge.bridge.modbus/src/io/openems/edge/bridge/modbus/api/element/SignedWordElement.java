package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;
import java.util.Optional;

import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import io.openems.common.exceptions.OpenemsException;

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
		// set value
		super.setValue(shortValue);
	}

	@Override
	public void _setNextWriteValue(Optional<Integer> valueOpt) throws OpenemsException {
		if (valueOpt.isPresent()) {
			byte[] b = ByteBuffer.allocate(2).order(this.getByteOrder()).putShort(valueOpt.get().shortValue()).array();
			this.setNextWriteValueRegisters(Optional.of(new Register[] { //
					new SimpleRegister(b[0], b[1]) }));
		} else {
			this.setNextWriteValueRegisters(Optional.empty());
		}
	}
}
