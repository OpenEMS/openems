package io.openems.edge.battery.fenecon.f2b.cluster.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.battery.fenecon.f2b.DeviceSpecificOnSetNextValueHandler.OnSetNextValueCallback;
import io.openems.edge.battery.fenecon.f2b.cluster.serial.BatteryFeneconF2bClusterSerialImpl;
import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;

public class ChannelManager extends AbstractChannelListenerManager {

	private final Battery parent;

	private static final BiFunction<Integer, Integer, Integer> INTEGER_MIN = TypeUtils::min;
	private static final BiFunction<Integer, Integer, Integer> INTEGER_MAX = TypeUtils::max;
	private static final BiFunction<Integer, Integer, Integer> INTEGER_SUM = TypeUtils::sum;
	private static final BiFunction<Integer, Integer, Integer> INTEGER_AVG = TypeUtils::averageInt;

	public ChannelManager(Battery parent) {
		this.parent = parent;
	}

	/**
	 * Called on Component activate().
	 * 
	 * @param batteries the List of {@link Battery}
	 * @param cluster   the battery f2b cluster {@link BatteryFeneconF2bCluster}
	 */
	public void activate(List<BatteryFeneconF2b> batteries, BatteryFeneconF2bCluster cluster) {
		this.calculate(INTEGER_AVG, batteries, Battery.ChannelId.SOC);
		this.calculate(INTEGER_AVG, batteries, Battery.ChannelId.SOH);
		this.calculate(INTEGER_AVG, batteries, BatteryFeneconF2b.ChannelId.AVG_CELL_TEMPERATURE);
		this.calculate(INTEGER_SUM, batteries, Battery.ChannelId.CAPACITY);
		this.calculate(INTEGER_MIN, batteries, Battery.ChannelId.MIN_CELL_TEMPERATURE);
		this.calculate(INTEGER_MIN, batteries, Battery.ChannelId.MIN_CELL_VOLTAGE);
		this.calculate(INTEGER_MAX, batteries, Battery.ChannelId.MAX_CELL_TEMPERATURE);
		this.calculate(INTEGER_MAX, batteries, Battery.ChannelId.MAX_CELL_VOLTAGE);

		// TODO this is not compatible with different types of Batteries
		var handler = batteries.stream() //
				.map(BatteryFeneconF2b::getDeviceSpecificOnSetNextValueHandler)//
				.filter(Objects::nonNull)//
				.findFirst().orElse(null);

		if (handler == null) {
			return;
		}
		var onSetNextValueCallbacks = handler.getOnSetNextValueCallbacks();
		for (var onSetNextValueCallback : onSetNextValueCallbacks) {
			this.addOnSetNextValueListener(onSetNextValueCallback, batteries, cluster);
		}
	}

	protected <T> void addOnSetNextValueListener(OnSetNextValueCallback onSetNextValueCallback,
			List<BatteryFeneconF2b> batteries, BatteryFeneconF2bCluster cluster) {
		final Consumer<Value<T>> callback = (ignore) -> {
			onSetNextValueCallback.callback().accept(batteries, cluster);
		};

		for (var battery : batteries) {
			for (var channelId : onSetNextValueCallback.channelIds()) {
				this.addOnSetNextValueListener(battery, channelId, callback);
			}
		}
	}

	/**
	 * Aggregate Channels of {@link Battery}s.
	 *
	 * @param <T>        the Channel Type
	 * @param aggregator the aggregator function
	 * @param batteries  the List of {@link Battery}
	 * @param channelId  the Battery.ChannelId
	 */
	protected <T> void calculate(BiFunction<T, T, T> aggregator, List<BatteryFeneconF2b> batteries,
			io.openems.edge.common.channel.ChannelId channelId) {
		final Consumer<Value<Boolean>> callback = (value) -> {
			T result = null;
			for (var battery : batteries) {
				Channel<T> channel = battery.channel(channelId);
				result = aggregator.apply(result, channel.getNextValue().get());
			}
			var channel = this.parent.channel(channelId);
			channel.setNextValue(result);
		};
		for (var battery : batteries) {
			this.addOnSetNextValueListener(battery, channelId, callback);
		}
	}

	protected void setBatteryVoltageLimits(List<BatteryFeneconF2b> batteries, //
			Battery.ChannelId firstChannelId, //
			Battery.ChannelId secondChannelId, //
			BiFunction<Integer, Integer, Integer> channelsMethod, //
			BiFunction<Integer, Integer, Integer> resultMethod, //
			Consumer<Integer> batteryMethod) {
		this.setBatteryLimits(batteries, firstChannelId, secondChannelId, channelsMethod, resultMethod, batteryMethod,
				TypeUtils::abs);
	}

	protected void setBatteryCurrentLimits(List<BatteryFeneconF2b> batteries, //
			Battery.ChannelId firstChannelId, //
			Battery.ChannelId secondChannelId, //
			BiFunction<Integer, Integer, Integer> channelsMethod, //
			BiFunction<Integer, Integer, Integer> resultMethod, //
			Consumer<Integer> batteryMethod) {
		this.setBatteryLimits(batteries, firstChannelId, secondChannelId, channelsMethod, resultMethod, batteryMethod,
				Function.identity());
	}

	/**
	 * For each battery, according to the given method {@code "channelsMethod"}
	 * first and second channel actual value calculates and each calculated value
	 * put in a list. Gets the one with minimum difference,which is called as delta
	 * minimum. Multiplied with the size of list. And applied {@code "resultMethod"}
	 * method to the sum of battery's second channels.
	 * 
	 * @param batteries       The list of {@link Battery}
	 * @param firstChannelId  first channelId the {@link Battery.ChannelId}.
	 * @param secondChannelId second channelId the {@link Battery.ChannelId} to be
	 *                        sum each battery value.
	 * @param channelsMethod  A {@link TypeUtils} method to be applied between
	 *                        battery channels, e.g. sum, subtract
	 * @param resultMethod    A {@link TypeUtils} method to be applied between sum
	 *                        of second channels result and delta minimum.
	 * @param batteryMethod   A default {@link Battery} interface method to set the
	 *                        value on required channel.
	 * @param mapper          Only to decide whether {@link TypeUtils#abs} method
	 *                        necessary. The main difference comes from voltage(only
	 *                        positive) and current(can be negative) values.
	 */
	protected void setBatteryLimits(//
			List<BatteryFeneconF2b> batteries, //
			Battery.ChannelId firstChannelId, //
			Battery.ChannelId secondChannelId, //
			BiFunction<Integer, Integer, Integer> channelsMethod, //
			BiFunction<Integer, Integer, Integer> resultMethod, //
			Consumer<Integer> batteryMethod, //
			Function<Integer, Integer> mapper //
	) {
		final Consumer<Value<Integer>> callback = (value) -> {
			if (!this.parent.isStarted()) {
				batteryMethod.accept(null);
				return;
			}
			Integer sumOfSecondChannelValues = null;
			List<Integer> differences = new ArrayList<>();
			for (var battery : batteries) {
				IntegerReadChannel firstChannel = battery.channel(firstChannelId);
				IntegerReadChannel secondChannel = battery.channel(secondChannelId);
				var firstChannelValue = firstChannel.getNextValue();
				var secondChannelValue = secondChannel.getNextValue();
				if (this.parent instanceof BatteryFeneconF2bClusterSerialImpl
						&& (!firstChannelValue.isDefined() || !secondChannelValue.isDefined())) {
					batteryMethod.accept(null);
					return;
				}
				var deltaMin = mapper.apply(//
						channelsMethod.apply(//
								firstChannelValue.get(), //
								secondChannelValue.get()));
				if (deltaMin != null) {
					differences.add(deltaMin);
				}
				sumOfSecondChannelValues = TypeUtils.sum(//
						sumOfSecondChannelValues, //
						secondChannelValue.get());
			}
			if (sumOfSecondChannelValues != null && !differences.isEmpty()) {
				var minDifference = differences.stream().min(Integer::compareTo).get();
				var limitValue = TypeUtils.abs(resultMethod.apply(//
						sumOfSecondChannelValues, //
						TypeUtils.multiply(batteries.size(), minDifference)));
				batteryMethod.accept(TypeUtils.averageInt(limitValue));
			}
		};

		for (var battery : batteries) {
			this.addOnSetNextValueListener(battery, firstChannelId, callback);
		}
	}

}
