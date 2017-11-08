package io.openems.impl.controller.api.modbustcp;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.doc.ChannelDoc;
import io.openems.api.exception.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.BitUtils;

public class ChannelRegisterMap {

	private final ChannelAddress channelAddress;
	private final ChannelDoc channelDoc;
	private final MyRegister[] registers;
	/**
	 * setBuffer is used to buffer calls to MyRegister.setValue. Once the buffer is full, the value is actually set on
	 * the channel.
	 */
	private final Byte[] setBuffer;

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
		this.registers = new MyRegister[registerLength];
		for (int i = 0; i < registerLength; i++) {
			this.registers[i] = new MyRegister(this, i);
		}
		// initialize setBuffer
		this.setBuffer = new Byte[registerLength * 2];
		this.initializeSetBuffer();
	}

	public MyRegister[] getRegisters() {
		return this.registers;
	}

	protected ChannelAddress getChannelAddress() {
		return this.channelAddress;
	}

	protected void setValue(MyRegister register, byte b1, byte b2) throws OpenemsException {
		System.out.println("setValue " + b1 + " " + b2);
		int registerNo = register.getRegisterNo();
		this.setBuffer[registerNo * 2] = b1;
		this.setBuffer[registerNo * 2 + 1] = b2;
		// is the buffer full?
		for (int i = 0; i < this.setBuffer.length; i++) {
			if(this.setBuffer[i] == null || this.setBuffer[i+1] == null) {
				return; // no, it is not full
			}
		}
		// yes, it is full
		Channel channel = this.getChannel();
		if(channel instanceof ConfigChannel<?>) {
			ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
			byte[] value = new byte[this.setBuffer.length];
			for(int i=0; i<this.setBuffer.length; i++) {
				value[i] = this.setBuffer[i];
			}
			Object valueObj = BitUtils.toObject(this.channelDoc.getTypeOpt().get(), value);
			configChannel.updateValue(valueObj, true);
		}
	}

	private void initializeSetBuffer() {
		for (int i = 0; i < this.setBuffer.length; i++) {
			this.setBuffer[i] = null;
		}
	}

	/**
	 * Returns the channel for the set ChannelAddress
	 *
	 * @return
	 * @throws OpenemsException
	 */
	protected Channel getChannel() throws OpenemsException {
		Optional<Channel> channelOpt = ThingRepository.getInstance().getChannel(this.channelAddress);
		if (!channelOpt.isPresent()) {
			throw new OpenemsException("Channel does not exist [" + this.channelAddress + "]");
		}
		Channel channel = channelOpt.get();
		return channel;
	}
}
