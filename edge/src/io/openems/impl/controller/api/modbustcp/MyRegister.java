package io.openems.impl.controller.api.modbustcp;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.api.channel.Channel;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.OpenemsException;
import io.openems.core.Databus;
import io.openems.core.utilities.BitUtils;

public class MyRegister implements Register {

	private final Logger log = LoggerFactory.getLogger(MyRegister.class);
	private final static byte[] VALUE_ON_ERROR = new byte[] { 0, 0 };

	private final ChannelRegisterMap parent;
	/**
	 * The register number of this Register within its parent
	 */
	private final int registerNo;

	public MyRegister(ChannelRegisterMap parent, int registerNo) {
		this.parent = parent;
		this.registerNo = registerNo;
	}

	public int getRegisterNo() {
		return registerNo;
	}

	@Override
	public int getValue() {
		// TODO
		log.warn("getValue is not implemented");
		return 0;
	}

	@Override
	public int toUnsignedShort() {
		// TODO
		log.warn("toUnsignedShort is not implemented");
		return 0;
	}

	@Override
	public short toShort() {
		// TODO
		log.warn("toShort is not implemented");
		return 0;
	}

	@Override
	public byte[] toBytes() {
		Channel channel;
		try {
			channel = parent.getChannel();
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
			return VALUE_ON_ERROR;
		}
		Optional<?> valueOpt = Databus.getInstance().getValue(channel);
		if (valueOpt.isPresent()) {
			// we got a value
			Object object = valueOpt.get();
			try {
				byte[] b = BitUtils.toBytes(object);
				return new byte[] { b[this.registerNo * 2], b[this.registerNo * 2 + 1] };
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
		return VALUE_ON_ERROR;
	}

	@Override
	public void setValue(int v) {
		this.setValue((short) v);
	}

	@Override
	public void setValue(short s) {
		try {
			byte[] bytes = BitUtils.toBytes(s);
			this.setValue(bytes);
		} catch (NotImplementedException e) { /* will not happen */}
	}

	@Override
	public void setValue(byte[] bytes) {
		try {
			this.parent.setValue(this, bytes[0], bytes[1]);
		} catch (OpenemsException e) {
			log.warn(e.getMessage());
		}
	}
}
