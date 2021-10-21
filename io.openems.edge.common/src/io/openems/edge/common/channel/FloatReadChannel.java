package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class FloatReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Float>, Float> {

	protected FloatReadChannel(OpenemsComponent component, ChannelId channelId, FloatDoc channelDoc) {
		super(OpenemsType.FLOAT, component, channelId, channelDoc);
	}

}
