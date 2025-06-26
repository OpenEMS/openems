package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class IntegerDoc extends OpenemsTypeDoc<Integer> {

	public IntegerDoc() {
		super(OpenemsType.INTEGER);
	}

	@Override
	protected IntegerDoc self() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public IntegerReadChannel createChannelInstance(OpenemsComponent component, ChannelId channelId) {
		return switch (this.getAccessMode()) {
		case READ_ONLY //
			-> new IntegerReadChannel(component, channelId, this);
		case READ_WRITE, WRITE_ONLY //
			-> new IntegerWriteChannel(component, channelId, this);
		};
	}
}
