package io.openems.edge.meter.virtual.common;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.SymmetricMeter;

public class ChannelManager extends AbstractChannelListenerManager {

	private final SymmetricMeter parent;

	public SymmetricMeter getParent() {
		return this.parent;
	}

	public static final BiFunction<Integer, Integer, Integer> INTEGER_SUM = TypeUtils::sum;
	public static final BiFunction<Long, Long, Long> LONG_SUM = TypeUtils::sum;
	public static final BiFunction<Integer, Integer, Integer> INTEGER_AVG = TypeUtils::averageInt;

	public ChannelManager(SymmetricMeter parent) {
		this.parent = parent;
	}

	/**
	 * Called on Component activate().
	 *
	 * @param meters the List of < ? extends {@link SymmetricMeter}>
	 * 
	 */
	public void activate(List<? extends SymmetricMeter> meters) {
		this.calculate(INTEGER_SUM, meters, SymmetricMeter.ChannelId.ACTIVE_POWER);
		this.calculate(INTEGER_SUM, meters, SymmetricMeter.ChannelId.REACTIVE_POWER);
		this.calculate(LONG_SUM, meters, SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
		this.calculate(LONG_SUM, meters, SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);
		this.calculate(INTEGER_SUM, meters, SymmetricMeter.ChannelId.CURRENT);
		this.calculate(INTEGER_AVG, meters, SymmetricMeter.ChannelId.FREQUENCY);
		this.calculate(INTEGER_AVG, meters, SymmetricMeter.ChannelId.VOLTAGE);
	}

	/**
	 * Aggregate Channels of {@link SymmetricMeter}s.
	 *
	 * @param <T>        the Channel Type
	 * @param aggregator the aggregator function
	 * @param meters     the List of {@link SymmetricMeter}
	 * @param channelId  the SymmetricMeter.ChannelId
	 */
	private <T> void calculate(BiFunction<T, T, T> aggregator, //
			List<? extends SymmetricMeter> meters, SymmetricMeter.ChannelId channelId) {
		final BiConsumer<Value<T>, Value<T>> callback = (oldValue, newValue) -> {
			T result = null;
			for (SymmetricMeter meter : meters) {
				Channel<T> channel = meter.channel(channelId);
				result = aggregator.apply(result, channel.getNextValue().get());
			}
			Channel<T> channel = this.parent.channel(channelId);
			channel.setNextValue(result);
		};
		for (SymmetricMeter meter : meters) {
			this.addOnChangeListener(meter, channelId, callback);
		}
	}

}
