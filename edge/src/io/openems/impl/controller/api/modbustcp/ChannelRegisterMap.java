package io.openems.impl.controller.api.modbustcp;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.doc.ChannelDoc;
import io.openems.api.exception.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.BitUtils;
import io.openems.core.utilities.api.ApiWorker;
import io.openems.core.utilities.api.WritePOJO;

public class ChannelRegisterMap {

	private final Logger log = LoggerFactory.getLogger(ChannelRegisterMap.class);
	private final ChannelAddress channelAddress;
	private final ChannelDoc channelDoc;
	private final MyRegister[] registers;
	private final ApiWorker apiWorker;
	/**
	 * setBuffer is used to buffer calls to MyRegister.setValue. Once the buffer is full, the value is actually set on
	 * the channel.
	 */
	private final Byte[] setBuffer;

	public ChannelRegisterMap(ChannelAddress channelAddress, ChannelDoc channelDoc, ApiWorker apiWorker)
			throws OpenemsException {
		this.channelAddress = channelAddress;
		this.channelDoc = channelDoc;
		this.apiWorker = apiWorker;
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
		int registerNo = register.getRegisterNo();
		this.setBuffer[registerNo * 2] = b1;
		this.setBuffer[registerNo * 2 + 1] = b2;
		// is the buffer full?
		for (int i = 0; i < this.setBuffer.length; i++) {
			if (this.setBuffer[i] == null) {
				return; // no, it is not full
			}
		}
		// yes, it is full -> parse value to Object
		byte[] value = new byte[this.setBuffer.length];
		boolean isPositive = true;
		for (int i = this.setBuffer.length - 1; i > 0; i -= 2) {
			if (((this.setBuffer[i - 1] >>> 8) & 1) != 0) {
				// test if the 'negative-bit' is set
				isPositive = false;
			}
			value[i] = this.setBuffer[i];
			value[i - 1] = this.setBuffer[i - 1];
		}
		if(!isPositive) {
			// fill leading bytes with 0xFF if the value is negative
			for(int i = 0; i < value.length; i++) {
				if(value[i] != 0) {
					break;
				}
				value[i] = (byte)0xff;
			}
		}
		// int bitLength = getBitLength(type);
		// System.out.println(bitLength + " - " + bytesToHex(value));

		Object valueObj = BitUtils.toObject(this.channelDoc.getTypeOpt().get(), value);

		Channel channel = this.getChannel();
		if (channel instanceof ConfigChannel<?>) {
			// it is a ConfigChannel -> write directly
			ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
			configChannel.updateValue(valueObj, true);
			log.info("Updated [" + this.channelAddress + "] to [" + valueObj + "] via Modbus/TCP.");
		} else if (channel instanceof WriteChannel<?>) {
			// it is a WriteChannel -> handle by ApiWorker
			WriteChannel<?> writeChannel = (WriteChannel<?>) channel;
			WritePOJO writeObject = new WritePOJO(valueObj);
			apiWorker.addValue(writeChannel, writeObject);
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
