package io.openems.edge.common.channel;

import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class EnumReadChannel extends AbstractReadChannel<EnumDoc, Integer> {

	protected EnumReadChannel(OpenemsComponent component, ChannelId channelId, EnumDoc channelDoc) {
		super(OpenemsType.INTEGER, component, channelId, channelDoc);
	}

	protected EnumReadChannel(OpenemsComponent component, ChannelId channelId, EnumDoc channelDoc,
			OptionsEnum initialValue) {
		// Explicitly sets the initial Value
		super(OpenemsType.INTEGER, component, channelId, channelDoc.initialValue(initialValue));
	}

}
