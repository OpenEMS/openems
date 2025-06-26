package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.component.OpenemsComponent;

public class DoubleReadChannel extends AbstractReadChannel<OpenemsTypeDoc<Double>, Double> {

	protected DoubleReadChannel(OpenemsComponent component, ChannelId channelId, DoubleDoc channelDoc) {
		super(OpenemsType.DOUBLE, component, channelId, channelDoc);
	}

}
