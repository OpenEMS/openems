package io.openems.api.channel;

import java.util.Optional;

import io.openems.api.thing.Thing;

public class FunctionalChannel<T> extends ReadChannel<T> implements ChannelUpdateListener {

	private ReadChannel<T>[] channels;
	private FunctionalChannelFunction<T> func;

	public FunctionalChannel(String id, Thing parent, FunctionalChannelFunction<T> function,
			ReadChannel<T>... channels) {
		super(id, parent);
		this.channels = channels;
		this.func = function;
		for (Channel c : channels) {
			c.addUpdateListener(this);
		}
	}

	@Override
	public void channelUpdated(Channel channel, Optional<?> newValue) {
		updateValue(func.handle(channels));
	}

	@Override
	public FunctionalChannel<T> label(T value, String label) {
		super.label(value, label);
		return this;
	}
}
