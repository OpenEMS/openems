package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class IntegerDoc extends OpenemsTypeDoc<Integer> {

	public IntegerDoc() {
		super(OpenemsType.INTEGER);
	}

	@Override
	protected IntegerDoc self() {
		return this;
	}

	@Override
	public IntegerReadChannel createChannelInstance(OpenemsComponent component, ChannelId channelId) {
		switch (this.getAccessMode()) {
		case READ_ONLY:
			return new IntegerReadChannel(component, channelId, this);
		case READ_WRITE:
		case WRITE_ONLY:
			return new IntegerWriteChannel(component, channelId, this);
		}
		throw new IllegalArgumentException(
				"AccessMode [" + this.getAccessMode() + "] is unhandled. This should never happen.");
	}
}
