package io.openems.edge.meter.virtual.asymmetric.add;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.virtual.symmetric.add.SymmetricChannelManager;

public class AsymmetricChannelManager extends SymmetricChannelManager {

	public AsymmetricChannelManager(SymmetricMeter parent) {
		super(parent);
	}

	/**
	 * Called on Component activate().
	 *
	 * @param meters the {@link List} of < ? extends {@link SymmetricMeter}>
	 * 
	 */
	public void activate(List<? extends SymmetricMeter> meters) {
		// Calculate the symmetric meter channels
		super.activate(meters);

		List<AsymmetricMeter> asymmetricMeters = new ArrayList<>();

		for (SymmetricMeter meter : meters) {

			if (meter instanceof AsymmetricMeter) {
				asymmetricMeters.add((AsymmetricMeter) meter);
			}
		}

		this.calculate(INTEGER_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.ACTIVE_POWER_L1);
		this.calculate(INTEGER_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.ACTIVE_POWER_L2);
		this.calculate(INTEGER_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.ACTIVE_POWER_L3);

		this.calculate(INTEGER_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.REACTIVE_POWER_L1);
		this.calculate(INTEGER_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.REACTIVE_POWER_L2);
		this.calculate(INTEGER_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.REACTIVE_POWER_L3);

		this.calculate(LONG_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1);
		this.calculate(LONG_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2);
		this.calculate(LONG_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3);

		this.calculate(LONG_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
		this.calculate(LONG_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
		this.calculate(LONG_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

		this.calculate(INTEGER_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.CURRENT_L1);
		this.calculate(INTEGER_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.CURRENT_L2);
		this.calculate(INTEGER_SUM, asymmetricMeters, AsymmetricMeter.ChannelId.CURRENT_L3);

		this.calculate(INTEGER_AVG, asymmetricMeters, AsymmetricMeter.ChannelId.VOLTAGE_L1);
		this.calculate(INTEGER_AVG, asymmetricMeters, AsymmetricMeter.ChannelId.VOLTAGE_L2);
		this.calculate(INTEGER_AVG, asymmetricMeters, AsymmetricMeter.ChannelId.VOLTAGE_L3);

	}

	/**
	 * Aggregate Channels of {@link AsymmetricMeter}s.
	 *
	 * @param <T>        the Channel Type
	 * @param aggregator the aggregator function
	 * @param meters     the List of {@link AsymmetricMeter}
	 * @param channelId  the AsymmetricMeter.ChannelId
	 */
	private <T> void calculate(BiFunction<T, T, T> aggregator, //
			List<? extends AsymmetricMeter> meters, //
			AsymmetricMeter.ChannelId channelId) {

		final BiConsumer<Value<T>, Value<T>> callback = (oldValue, newValue) -> {
			T result = null;
			for (AsymmetricMeter meter : meters) {
				Channel<T> channel = meter.channel(channelId);
				result = aggregator.apply(result, channel.getNextValue().get());
			}
			Channel<T> channel = this.parent.channel(channelId);
			channel.setNextValue(result);
		};
		for (AsymmetricMeter meter : meters) {
			this.addOnChangeListener(meter, channelId, callback);
		}
	}

}
