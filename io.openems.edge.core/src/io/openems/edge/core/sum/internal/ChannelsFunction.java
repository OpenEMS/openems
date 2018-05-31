package io.openems.edge.core.sum.internal;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.sum.Sum;

public abstract class ChannelsFunction<T> {

	private final Channel<T> targetChannel;
	private final ChannelId sourceChannelId;

	protected final Map<String, Value<T>> valueMap = new ConcurrentHashMap<>();

	public ChannelsFunction(Sum parent, io.openems.edge.core.sum.Sum.ChannelId targetChannelId,
			ChannelId sourceChannelId) {
		this.targetChannel = parent.channel(targetChannelId);
		this.sourceChannelId = sourceChannelId;
	}

	public void addComponent(OpenemsComponent component) {
		final Consumer<Value<T>> handler = value -> {
			this.valueMap.put(component.id(), value);
			try {
				this.targetChannel.setNextValue(this.calculate());
			} catch (NoSuchElementException e) {
				this.targetChannel.setNextValue(null);
			}
		};
		Channel<T> channel = component.channel(this.sourceChannelId);
		handler.accept(channel.value()); // handle current value
		channel.onSetNextValue(handler); // and every upcoming value
	}

	public void removeComponent(OpenemsComponent component) {
		this.valueMap.remove(component.id());
	}

	protected abstract double calculate() throws NoSuchElementException;
}
