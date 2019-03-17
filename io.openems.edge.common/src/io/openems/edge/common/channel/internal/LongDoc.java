package io.openems.edge.common.channel.internal;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.LongWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class LongDoc extends OpenemsTypeDoc<Long> {

	public LongDoc() {
		super(OpenemsType.LONG);
	}

	@Override
	protected LongDoc self() {
		return this;
	}

	@Override
	public LongReadChannel createChannelInstance(OpenemsComponent component, ChannelId channelId) {
		switch (this.getAccessMode()) {
		case READ_ONLY:
			return new LongReadChannel(component, channelId, this);
		case READ_WRITE:
		case WRITE_ONLY:
			return new LongWriteChannel(component, channelId, this);
		}
		throw new IllegalArgumentException(
				"AccessMode [" + this.getAccessMode() + "] is unhandled. This should never happen.");
	}
}
