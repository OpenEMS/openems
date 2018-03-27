package io.openems.edge.bridge.modbus.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;

public class UpdateChannel<T> implements OnUpdate<T> {

	private final Logger log = LoggerFactory.getLogger(UpdateChannel.class);
	private final Channel channel;

	public UpdateChannel(Channel channel) {
		this.channel = channel;
	}

	@Override
	public void call(T value) {
		try {
			this.channel.setNextValue(JsonUtils.getAsJsonElement(value));
		} catch (OpenemsException e) {
			log.warn("Channel [" + channel.address() + "]: Unable to set next value [" + value + "] ."
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

}
