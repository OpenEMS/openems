package io.openems.edge.common.channel;


import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class EnumReadChannel extends AbstractReadChannel<EnumDoc, Integer> {

	protected EnumReadChannel(OpenemsComponent component, ChannelId channelId, EnumDoc channelDoc) {
		this(component, channelId, channelDoc, null);
	}

	protected EnumReadChannel(OpenemsComponent component, ChannelId channelId, EnumDoc channelDoc,
			OptionsEnum optionsEnum) {
		super(OpenemsType.INTEGER, component, channelId, channelDoc, optionsEnum.getValue());
	}

}
