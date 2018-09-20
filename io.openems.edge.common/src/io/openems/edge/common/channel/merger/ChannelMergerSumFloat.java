package io.openems.edge.common.channel.merger;

import java.util.Collection;
import java.util.function.Function;

import io.openems.edge.common.channel.Channel;

/**
 * Calculates the sum of the source Channels and sets it as value for the target
 * Channel.
 */
public class ChannelMergerSumFloat extends ChannelMerger<Float> {

	private final static Function<Collection<Float>, Float> sumFunction = (sourceValues) -> {
		float sum = 0;
		for (Float sourceValue : sourceValues) {
			if (sourceValue != null) {
				sum += sourceValue;
			}
		}
		return sum;
	};

	public ChannelMergerSumFloat(Channel<Float> target, Channel<Float>[] sources) {
		super(ChannelMergerSumFloat.sumFunction, target, 0f, sources);
	}

}
