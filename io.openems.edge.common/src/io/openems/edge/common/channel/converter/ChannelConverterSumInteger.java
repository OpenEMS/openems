package io.openems.edge.common.channel.converter;

import java.util.Collection;
import java.util.function.Function;

import io.openems.edge.common.channel.Channel;

/**
 * Calculates the sum of the source Channels and sets it as value for the target
 * Channel.
 */
public class ChannelConverterSumInteger extends ChannelConverter<Integer> {

	private final static Function<Collection<Integer>, Integer> sumFunction = (sourceValues) -> {
		int sum = 0;
		for (int sourceValue : sourceValues) {
			sum += sourceValue;
		}
		return sum;
	};

	public ChannelConverterSumInteger(Channel<Integer> target, Channel<Integer>[] sources) {
		super(ChannelConverterSumInteger.sumFunction, target, 0, sources);
	}

}
