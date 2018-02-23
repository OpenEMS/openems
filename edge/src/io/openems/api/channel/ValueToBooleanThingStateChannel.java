package io.openems.api.channel;

import java.util.Optional;

import io.openems.api.channel.thingstate.ThingStateEnum;
import io.openems.api.thing.Thing;

public class ValueToBooleanThingStateChannel extends ThingStateChannel implements ChannelChangeListener{

	private ReadChannel<? extends Number> valueChannel;
	private long value;

	public ValueToBooleanThingStateChannel(ThingStateEnum state, Thing parent, ReadChannel<? extends Number> channel, long value) {
		super(state, parent);
		this.valueChannel = channel;
		this.valueChannel.addChangeListener(this);
		this.value = value;
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if(valueChannel.isValuePresent()) {
			if(valueChannel.getValue().longValue() == value) {
				updateValue(true);
			}else {
				updateValue(false);
			}
		}
	}

}
