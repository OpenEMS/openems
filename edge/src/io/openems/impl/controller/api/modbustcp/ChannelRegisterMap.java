package io.openems.impl.controller.api.modbustcp;

import java.util.Optional;

import io.openems.api.doc.ChannelDoc;
import io.openems.api.exception.OpenemsException;
import io.openems.common.types.ChannelAddress;

public class ChannelRegisterMap {

	private final ChannelAddress channelAddress;
	private final ChannelDoc channelDoc;
	private final Register[] registers;

	public ChannelRegisterMap(ChannelAddress channelAddress, ChannelDoc channelDoc) throws OpenemsException {
		this.channelAddress = channelAddress;
		this.channelDoc = channelDoc;
		// check type + bitLength
		Optional<Class<?>> typeOpt = channelDoc.getTypeOpt();
		if (!typeOpt.isPresent()) {
			throw new OpenemsException(
					"Type for Channel [" + channelAddress + "] is not defined. Annotation is missing.");
		}
		Optional<Integer> bitLengthOpt = channelDoc.getBitLengthOpt();
		if (!bitLengthOpt.isPresent()) {
			throw new OpenemsException("BitLength for Channel [" + channelAddress + "] is not defined.");
		}
		int registerLength = bitLengthOpt.get() / 16;
		// initialize registers
		this.registers = new Register[registerLength];
		for (int i = 0; i < registerLength; i++) {
			this.registers[i] = new ReadRegister(this, i);
		}
	}

	public int registerCount() {
		return this.registers.length;
	}

	public ReadRegister[] getReadRegisters() throws OpenemsException {
		ReadRegister[] registers = new ReadRegister[this.registers.length];
		for (int i = 0; i < registers.length; i++) {
			if(!(this.registers[i] instanceof ReadRegister)) {
				throw new OpenemsException("Register for channel ["+this.channelAddress+"] is not a ReadRegister.");
			}
			registers[i] = (ReadRegister) this.registers[i];
		}
		return registers;
	}

	protected ChannelAddress getChannelAddress() {
		return this.channelAddress;
	}
}
