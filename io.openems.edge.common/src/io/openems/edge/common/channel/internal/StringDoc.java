package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class StringDoc extends OpenemsTypeDoc<String> {

	public StringDoc() {
		super(OpenemsType.STRING);
	}

	@Override
	protected StringDoc self() {
		return this;
	}

	@Override
	public StringReadChannel createChannelInstance(OpenemsComponent component, ChannelId channelId) {
		switch (this.getAccessMode()) {
		case READ_ONLY:
			return new StringReadChannel(component, channelId, this);
		case READ_WRITE:
		case WRITE_ONLY:
			return new StringWriteChannel(component, channelId, this);
		}
		throw new IllegalArgumentException(
				"AccessMode [" + this.getAccessMode() + "] is unhandled. This should never happen.");
	}
}
