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
		return switch (this.getAccessMode()) {
		case READ_ONLY //
			-> new StringReadChannel(component, channelId, this);
		case READ_WRITE, WRITE_ONLY //
			-> new StringWriteChannel(component, channelId, this);
		};
	}
}
