package io.openems.edge.batteryinverter.cluster;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.type.TypeUtils;

public class ChannelManager extends AbstractChannelListenerManager {

	private final BatteryInverterCluster parent;

	public ChannelManager(BatteryInverterCluster parent) {
		this.parent = parent;
	}

	protected void activate(List<SymmetricBatteryInverter> inverters) {
		this.calculateGridMode(inverters);
		this.calculate(INTEGER_SUM, inverters, SymmetricBatteryInverter.ChannelId.ACTIVE_POWER);
		this.calculate(INTEGER_SUM, inverters, SymmetricBatteryInverter.ChannelId.REACTIVE_POWER);
		this.calculate(INTEGER_SUM, inverters, SymmetricBatteryInverter.ChannelId.MAX_APPARENT_POWER);
		this.calculate(LONG_SUM, inverters, SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY);
		this.calculate(LONG_SUM, inverters, SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY);
	}

	private static final Function<Integer, Integer> DIVIDE_BY_THREE = value -> TypeUtils.divide(value, 3);
	private static final BiFunction<Integer, Integer, Integer> INTEGER_MIN = TypeUtils::min;
	private static final BiFunction<Integer, Integer, Integer> INTEGER_MAX = TypeUtils::max;
	private static final BiFunction<Integer, Integer, Integer> INTEGER_SUM = TypeUtils::sum;
	private static final BiFunction<Long, Long, Long> LONG_SUM = TypeUtils::sum;

	/**
	 * Calculate effective Grid-Mode of {@link SymmetricEss}.
	 *
	 * @param inverters the List of {@link SymmetricEss}
	 */
	private void calculateGridMode(List<SymmetricBatteryInverter> inverters) {
		final BiConsumer<Value<Integer>, Value<Integer>> callback = (oldValue, newValue) -> {
			var onGrids = 0;
			var offGrids = 0;
			for (SymmetricBatteryInverter inverter : inverters) {
				switch (inverter.getGridMode()) {
				case OFF_GRID:
					offGrids++;
					break;
				case ON_GRID:
					onGrids++;
					break;
				case UNDEFINED:
					break;
				}
			}
			final GridMode result;
			if (inverters.size() == onGrids) {
				result = GridMode.ON_GRID;
			} else if (inverters.size() == offGrids) {
				result = GridMode.OFF_GRID;
			} else {
				result = GridMode.UNDEFINED;
			}
			this.parent._setGridMode(result);
		};
		for (SymmetricBatteryInverter inverter : inverters) {
			this.addOnChangeListener(inverter, SymmetricBatteryInverter.ChannelId.GRID_MODE, callback);
		}
	}

	/**
	 * Aggregate Channels of {@link SymmetricBatteryInverter}s.
	 *
	 * @param <T>        the Channel Type
	 * @param aggregator the aggregator function
	 * @param inverters       the List of {@link SymmetricBatteryInverter}
	 * @param channelId  the SymmetricBatteryInverter.ChannelId
	 */
	private <T> void calculate(BiFunction<T, T, T> aggregator, List<SymmetricBatteryInverter> inverters,
			SymmetricBatteryInverter.ChannelId channelId) {
		final BiConsumer<Value<T>, Value<T>> callback = (oldValue, newValue) -> {
			T result = null;
			for (SymmetricBatteryInverter inverter : inverters) {
				Channel<T> channel = inverter.channel(channelId);
				result = aggregator.apply(result, channel.getNextValue().get());
			}
			Channel<T> channel = this.parent.channel(channelId);
			channel.setNextValue(result);
		};
		for (SymmetricBatteryInverter inverter : inverters) {
			this.addOnChangeListener(inverter, channelId, callback);
		}
	}
}
