package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class BooleanDoc extends OpenemsTypeDoc<Boolean> {

	public BooleanDoc() {
		super(OpenemsType.BOOLEAN);
	}

	@Override
	protected BooleanDoc self() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public BooleanReadChannel createChannelInstance(OpenemsComponent component, ChannelId channelId) {
		switch (this.getAccessMode()) {
		case READ_ONLY:
			return new BooleanReadChannel(component, channelId, this);
		case READ_WRITE:
		case WRITE_ONLY:
			return new BooleanWriteChannel(component, channelId, this);
		}
		throw new IllegalArgumentException(
				"AccessMode [" + this.getAccessMode() + "] is unhandled. This should never happen.");
	}
}
