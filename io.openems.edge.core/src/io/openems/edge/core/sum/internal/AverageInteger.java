package io.openems.edge.core.sum.internal;

import java.util.NoSuchElementException;

import io.openems.edge.common.component.OpenemsComponent;

public class AverageInteger<C extends OpenemsComponent> extends ChannelsFunction<C, Integer> {

//	public AverageInteger(Sum parent, ChannelId targetChannelId,
//			io.openems.edge.common.channel.doc.ChannelId sourceChannelId) {
//		super(parent, targetChannelId, sourceChannelId);
//	}

	public AverageInteger(OpenemsComponent parent, io.openems.edge.common.channel.doc.ChannelId target,
			io.openems.edge.common.channel.doc.ChannelId source) {
		super(parent, target, source);
	}

	protected double calculate() throws NoSuchElementException {
		return this.valueMap.values() //
				.stream() //
				.filter(v -> v.asOptional().isPresent()) //
				.mapToDouble(v -> v.get()) //
				.average() //
				.getAsDouble();
	}
}
