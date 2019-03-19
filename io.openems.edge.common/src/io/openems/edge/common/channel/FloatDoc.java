package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class FloatDoc extends OpenemsTypeDoc<Float> {

	public FloatDoc() {
		super(OpenemsType.FLOAT);
	}

	@Override
	protected FloatDoc self() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public FloatReadChannel createChannelInstance(OpenemsComponent component, ChannelId channelId) {
		switch (this.getAccessMode()) {
		case READ_ONLY:
			return new FloatReadChannel(component, channelId, this);
		case READ_WRITE:
		case WRITE_ONLY:
			return new FloatWriteChannel(component, channelId, this);
		}
		throw new IllegalArgumentException(
				"AccessMode [" + this.getAccessMode() + "] is unhandled. This should never happen.");
	}
}
