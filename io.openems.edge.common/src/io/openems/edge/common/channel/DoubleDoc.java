package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class DoubleDoc extends OpenemsTypeDoc<Double> {

	public DoubleDoc() {
		super(OpenemsType.DOUBLE);
	}

	@Override
	protected DoubleDoc self() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public DoubleReadChannel createChannelInstance(OpenemsComponent component, ChannelId channelId) {
		return switch (this.getAccessMode()) {
		case READ_ONLY //
			-> new DoubleReadChannel(component, channelId, this);
		case READ_WRITE, WRITE_ONLY //
			-> new DoubleWriteChannel(component, channelId, this);
		};
	}
}
