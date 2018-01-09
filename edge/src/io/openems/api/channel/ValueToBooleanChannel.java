package io.openems.api.channel;

import java.util.Optional;

import io.openems.api.thing.Thing;

public class ValueToBooleanChannel extends ReadChannel<Boolean> implements ChannelChangeListener{

	private ReadChannel<? extends Number> valueChannel;
	private long value;

	public ValueToBooleanChannel(String id, Thing parent, ReadChannel<? extends Number> channel, long value) {
		super(id, parent);
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
