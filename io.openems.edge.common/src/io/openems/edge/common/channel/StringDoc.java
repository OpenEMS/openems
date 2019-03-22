package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class StringDoc extends OpenemsTypeDoc<String> {

	public StringDoc() {
		super(OpenemsType.STRING);
	}

	@Override
	protected StringDoc self() {
		return this;
	}

	@SuppressWarnings("unchecked")
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
