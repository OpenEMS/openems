package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class LongDoc extends OpenemsTypeDoc<Long> {

	public LongDoc() {
		super(OpenemsType.LONG);
	}

	@Override
	protected LongDoc self() {
		return this;
	}

	@SuppressWarnings("unchecked")
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
