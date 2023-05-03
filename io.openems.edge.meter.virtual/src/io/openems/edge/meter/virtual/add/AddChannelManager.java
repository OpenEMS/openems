package io.openems.edge.meter.virtual.add;

import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.ElectricityMeter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class AddChannelManager extends AbstractChannelListenerManager {

	public static final Function<List<Integer>, Integer> INTEGER_SUM = TypeUtils::sumInt;
	public static final Function<List<Long>, Long> LONG_SUM = TypeUtils::sum;
	public static final Function<List<Integer>, Integer> INTEGER_AVG = TypeUtils::averageInt;

	protected final ElectricityMeter parent;

	public AddChannelManager(ElectricityMeter parent) {
		this.parent = parent;
	}

	/**
	 * Called on Component activate().
	 *
	 * @param meters the List of <{@link ElectricityMeter}>
	 * 
	 */
	public void activate(List<ElectricityMeter> meters) {
		this.calculate(INTEGER_AVG, meters, ElectricityMeter.ChannelId.FREQUENCY);

		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_POWER);
		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_POWER_L1);
		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_POWER_L2);
		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_POWER_L3);

		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.REACTIVE_POWER);
		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.REACTIVE_POWER_L1);
		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.REACTIVE_POWER_L2);
		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.REACTIVE_POWER_L3);

		this.calculate(LONG_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
		this.calculate(LONG_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1);
		this.calculate(LONG_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2);
		this.calculate(LONG_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3);

		this.calculate(LONG_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);
		this.calculate(LONG_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
		this.calculate(LONG_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
		this.calculate(LONG_SUM, meters, ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.CURRENT);
		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.CURRENT_L1);
		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.CURRENT_L2);
		this.calculate(INTEGER_SUM, meters, ElectricityMeter.ChannelId.CURRENT_L3);

		this.calculate(INTEGER_AVG, meters, ElectricityMeter.ChannelId.VOLTAGE);
		this.calculate(INTEGER_AVG, meters, ElectricityMeter.ChannelId.VOLTAGE_L1);
		this.calculate(INTEGER_AVG, meters, ElectricityMeter.ChannelId.VOLTAGE_L2);
		this.calculate(INTEGER_AVG, meters, ElectricityMeter.ChannelId.VOLTAGE_L3);
	}

	/**
	 * Deactivates and activates the component.
	 * 
	 * @param meters the List of <{@link ElectricityMeter}>
	 */
	public void update(List<ElectricityMeter> meters) {
		this.deactivate();
		this.activate(meters);
	}

	/**
	 * Aggregate Channels of {@link ElectricityMeter}s.
	 *
	 * @param <T>        the Channel Type
	 * @param aggregator the aggregator function
	 * @param meters     the List of {@link ElectricityMeter}
	 * @param channelId  the ElectricityMeter.ChannelId
	 */
	private <T> void calculate(Function<List<T>, T> aggregator, //
	                           List<? extends ElectricityMeter> meters, ElectricityMeter.ChannelId channelId) {
		final BiConsumer<Value<T>, Value<T>> callback = (oldValue, newValue) -> {
			List<T> values = new ArrayList<>();
			for (ElectricityMeter meter : meters) {
				Channel<T> channel = meter.channel(channelId);
				T channelValue = channel.getNextValue().get();
				if (channelValue != null) {
					values.add(channelValue);
				}
			}

			T result = aggregator.apply(values);

			Channel<T> channel = this.parent.channel(channelId);
			channel.setNextValue(result);
		};
		for (ElectricityMeter meter : meters) {
			this.addOnChangeListener(meter, channelId, callback);
		}
	}
}
