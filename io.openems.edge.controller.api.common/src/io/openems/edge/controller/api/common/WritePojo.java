package io.openems.edge.controller.api.common;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.WriteChannel;

/**
 * A Wrapper for writing a POJO (plain old java object) to a WriteChannel.
 */
public class WritePojo extends WriteObject {

	public final Object value;

	public WritePojo(Object value) {
		this.value = value;
	}

	@Override
	public void setNextWriteValue(WriteChannel<?> writeChannel) throws OpenemsNamedException {
		writeChannel.setNextWriteValueFromObject(this.value);
	}

	@Override
	public String valueToString() {
		return String.valueOf(this.value);
	}

	@Override
	public boolean isNull() {
		return this.value == null;
	}

}
