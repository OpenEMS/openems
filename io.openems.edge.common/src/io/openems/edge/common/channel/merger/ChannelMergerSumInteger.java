package io.openems.edge.common.channel.merger;

import java.util.Collection;
import java.util.function.Function;

import io.openems.edge.common.channel.Channel;

/**
 * Calculates the sum of the source Channels and sets it as value for the target
 * Channel.
 */
public class ChannelMergerSumInteger extends ChannelMerger<Integer> {

	private final static Function<Collection<Integer>, Integer> sumFunction = (sourceValues) -> {
		int sum = 0;
		for (Integer sourceValue : sourceValues) {
			if (sourceValue != null) {
				sum += sourceValue;
			}
		}
		return sum;
	};

	public ChannelMergerSumInteger(Channel<Integer> target, Channel<Integer>[] sources) {
		super(ChannelMergerSumInteger.sumFunction, target, 0, sources);
	}

}
