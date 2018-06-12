package io.openems.edge.core.sum.internal;

import java.util.NoSuchElementException;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.sum.Sum;
import io.openems.edge.core.sum.Sum.ChannelId;

public class SumInteger<C extends OpenemsComponent> extends ChannelsFunction<C, Integer> {

	public SumInteger(Sum parent, ChannelId targetChannelId,
			io.openems.edge.common.channel.doc.ChannelId sourceChannelId) {
		super(parent, targetChannelId, sourceChannelId);
	}

	protected double calculate() throws NoSuchElementException {
		return this.valueMap.values() //
				.stream() //
				.filter(v -> v.asOptional().isPresent()) //
				.mapToDouble(v -> v.get()) //
				.sum();
	}
}
