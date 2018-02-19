package io.openems.core.utilities.api;

import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.WriteChannelException;

public class WritePOJO extends WriteObject {

	public final Object value;

	public WritePOJO(Object value) {
		super();
		this.value = value;
	}

	@Override
	public void pushWrite(WriteChannel<?> writeChannel) throws WriteChannelException {
		writeChannel.pushWriteFromObject(this.value);
	}

	@Override
	public String valueToString() {
		return this.value.toString();
	}
}
