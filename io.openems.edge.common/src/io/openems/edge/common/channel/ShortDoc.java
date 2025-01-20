package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class ShortDoc extends OpenemsTypeDoc<Short> {

	public ShortDoc() {
		super(OpenemsType.SHORT);
	}

	@Override
	protected ShortDoc self() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ShortReadChannel createChannelInstance(OpenemsComponent component, ChannelId channelId) {
		return switch (this.getAccessMode()) {
		case READ_ONLY //
			-> new ShortReadChannel(component, channelId, this);
		case READ_WRITE, WRITE_ONLY //
			-> new ShortWriteChannel(component, channelId, this);
		};
	}
}
