package io.openems.impl.controller.api.modbustcp;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.api.channel.Channel;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.OpenemsException;
import io.openems.core.Databus;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.BitUtils;

public class ReadRegister extends Register implements InputRegister {

	private final Logger log = LoggerFactory.getLogger(ReadRegister.class);
	private final static byte[] VALUE_ON_ERROR = new byte[] { 0, 0 };

	private final ChannelRegisterMap parent;
	/**
	 * The register number of this Register within its parent
	 */
	private final int registerNo;

	public ReadRegister(ChannelRegisterMap parent, int registerNo) {
		this.parent = parent;
		this.registerNo = registerNo;
	}

	@Override
	public int getValue() {
		return 0;
	}

	@Override
	public int toUnsignedShort() {
		log.warn("toUnsignedShort is not implemented");
		return 0;
	}

	@Override
	public short toShort() {
		log.warn("toShort is not implemented");
		return 0;
	}

	@Override
	public byte[] toBytes() {
		Optional<Channel> channelOpt = ThingRepository.getInstance().getChannel(this.parent.getChannelAddress());
		if (!channelOpt.isPresent()) {
			try {
				throw new OpenemsException("Channel does not exist [" + this.parent.getChannelAddress() + "]");
			} catch (OpenemsException e) {
				log.warn(e.getMessage());
			}
		}
		Channel channel = channelOpt.get();
		Optional<?> valueOpt = Databus.getInstance().getValue(channel);
		byte[] value = VALUE_ON_ERROR;
		if (valueOpt.isPresent()) {
			// we got a value
			Object object = valueOpt.get();
			try {
				byte[] b = BitUtils.toBytes(object);
				value[0] = b[this.registerNo * 2];
				value[1] = b[this.registerNo * 2 + 1];
			} catch (NotImplementedException e) {
				// unable to convert value to byte
				try {
					throw new OpenemsException("Unable to convert Channel [" + this.parent.getChannelAddress()
					+ "] value [" + object + "] to byte format.");
				} catch (OpenemsException e2) {
					log.warn(e2.getMessage());
				}
			}
		} else {
			// we got no value
			try {
				throw new OpenemsException(
						"Value for Channel [" + this.parent.getChannelAddress() + "] is not available.");
			} catch (OpenemsException e) {
				log.warn(e.getMessage());
			}
		}
		return value;
	}
}
