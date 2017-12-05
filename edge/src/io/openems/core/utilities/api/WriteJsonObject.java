package io.openems.core.utilities.api;

import com.google.gson.JsonElement;

import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.OpenemsException;

public class WriteJsonObject extends WriteObject {

	public final JsonElement jValue;

	public WriteJsonObject(JsonElement jValue) {
		super();
		this.jValue = jValue;
	}

	@Override
	public void pushWrite(WriteChannel<?> writeChannel) throws OpenemsException {
		writeChannel.pushWrite(this.jValue);
	}

	@Override
	public String valueToString() {
		return jValue.toString();
	}
}
