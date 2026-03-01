package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

abstract class ChannelParameterProvider<V> implements ParameterProvider {
	protected final ChannelId channelId;
	private final AtomicReference<V> value;

	ChannelParameterProvider(ChannelId channelId) {
		this.channelId = channelId;
		this.value = new AtomicReference<>(null);
	}

	@Override
	public void init(OpenemsComponent component) {
		var listener = this.getChannelListener();

		final Channel<V> channel = component.channel(this.channelId);
		channel.onChange(listener);
		channel.addOnDeactivateCallback(() -> channel.removeOnChangeCallback(listener));

		Value<V> currentValue = channel.getNextValue();
		if (currentValue.isDefined()) {
			listener.accept(null, currentValue);
		}
	}

	private BiConsumer<Value<V>, Value<V>> getChannelListener() {
		return (oldValue, newValue) -> {
			this.value.set(newValue.get());
		};
	}

	protected V getChannelValue() {
		return this.value.get();
	}

	protected String getChannelValueAsString() {
		var value = this.getChannelValue();
		if (value == null) {
			return "<>";
		}
		return value.toString();
	}

	@Override
	public abstract ParameterProvider clone();
}