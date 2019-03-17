package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.ShortReadChannel;
import io.openems.edge.common.channel.ShortWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class ShortDoc extends OpenemsTypeDoc<Short> {

	public ShortDoc() {
		super(OpenemsType.SHORT);
	}

	@Override
	protected ShortDoc self() {
		return this;
	}

	@Override
	public ShortReadChannel createChannelInstance(OpenemsComponent component, ChannelId channelId) {
		switch (this.getAccessMode()) {
		case READ_ONLY:
			return new ShortReadChannel(component, channelId, this);
		case READ_WRITE:
		case WRITE_ONLY:
			return new ShortWriteChannel(component, channelId, this);
		}
		throw new IllegalArgumentException(
				"AccessMode [" + this.getAccessMode() + "] is unhandled. This should never happen.");
	}
}
