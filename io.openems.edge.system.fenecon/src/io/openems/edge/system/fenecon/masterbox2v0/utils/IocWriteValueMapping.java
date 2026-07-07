package io.openems.edge.system.fenecon.masterbox2v0.utils;

import java.util.function.Supplier;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.common.channel.WriteChannel;

public class IocWriteValueMapping<T> {
	private final ThrowingConsumer<T, OpenemsError.OpenemsNamedException> listener;
	private final Supplier<WriteChannel<T>> channelGetter;

	public IocWriteValueMapping(ThrowingConsumer<T, OpenemsError.OpenemsNamedException> listener,
			Supplier<WriteChannel<T>> channelGetter) {
		this.listener = listener;
		this.channelGetter = channelGetter;
	}

	protected void addListener() {
		this.channelGetter.get().onSetNextWrite(this.listener);
	}

	protected void removeListener() {
		this.channelGetter.get().removeOnSetNextWriteCallback(this.listener);
	}
}
