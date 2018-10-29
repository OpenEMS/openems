package io.openems.edge.controller.api.core;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.WriteChannel;

public class WritePOJO extends WriteObject {

	public final Object value;

	public WritePOJO(Object value) {
		super();
		this.value = value;
	}

	@Override
	public void setNextWriteValue(WriteChannel<?> writeChannel) throws OpenemsException {
		writeChannel.setNextWriteValueFromObject(this.value);
	}

	@Override
	public String valueToString() {
		return this.value.toString();
	}

}
