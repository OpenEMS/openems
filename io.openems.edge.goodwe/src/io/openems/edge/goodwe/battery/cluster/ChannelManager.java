package io.openems.edge.goodwe.battery.cluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.channel.ChannelCategory;
import io.openems.common.types.Tuple2;
import io.openems.common.utils.IntUtils;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.AbstractChannelListenerManager;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;

public class ChannelManager extends AbstractChannelListenerManager {

	private final Battery parent;

	private static final BinaryOperator<Integer> INTEGER_MIN = IntUtils::minInteger;
	private static final BinaryOperator<Integer> INTEGER_MAX = IntUtils::maxInteger;
	private static final BinaryOperator<Integer> INTEGER_SUM = IntUtils::sumInteger;
	private static final BinaryOperator<Integer> INTEGER_AVG = TypeUtils::averageInt;
	private static final BinaryOperator<Integer> INTEGER_SUM_NEGATIVE = (integer, integer2) -> {
		if ((integer != null && integer < 0) || (integer2 != null && integer2 < 0)) {
			return IntUtils.minInt(0, integer) + IntUtils.minInt(0, integer2);
		}
		return IntUtils.sumInteger(integer, integer2);
	};

	public ChannelManager(Battery parent) {
		this.parent = parent;
	}

	/**
	 * Called on Component activate().
	 * 
	 * @param batteries the List of {@link Battery}
	 */
	public void activate(List<Battery> batteries) {
		this.addOnDeactivateListener(ChannelUtils.<Battery, Integer, Integer>subscribeOnSetNextValue(batteries,
				Battery.ChannelId.SOC, Battery.ChannelId.CAPACITY, batteryValueMap -> {
					final var soc = calculateSoc(batteries, batteryValueMap);
					this.parent._setSoc(soc);
				}));

		this.calculate(INTEGER_AVG, batteries, Battery.ChannelId.SOH);
		this.calculate(INTEGER_SUM, batteries, Battery.ChannelId.CAPACITY);
		this.calculate(INTEGER_MIN, batteries, Battery.ChannelId.MIN_CELL_TEMPERATURE);
		this.calculate(INTEGER_MIN, batteries, Battery.ChannelId.MIN_CELL_VOLTAGE);
		this.calculate(INTEGER_MAX, batteries, Battery.ChannelId.MAX_CELL_TEMPERATURE);
		this.calculate(INTEGER_MAX, batteries, Battery.ChannelId.MAX_CELL_VOLTAGE);
		this.calculate(INTEGER_AVG, batteries, Battery.ChannelId.VOLTAGE);
		this.calculate(INTEGER_SUM, batteries, Battery.ChannelId.CURRENT);
		this.calculate(INTEGER_MIN, batteries, Battery.ChannelId.CHARGE_MAX_VOLTAGE);
		this.calculate(INTEGER_MAX, batteries, Battery.ChannelId.DISCHARGE_MIN_VOLTAGE);

		this.<StartStop>calculate((startStop1, startStop2) -> {
			if (startStop1 == StartStop.START && startStop2 == StartStop.START) {
				return StartStop.START;
			}
			if (startStop1 == StartStop.STOP && startStop2 == StartStop.STOP) {
				return StartStop.STOP;
			}
			return StartStop.UNDEFINED;
		}, batteries, StartStoppable.ChannelId.START_STOP);

		this.calculate(INTEGER_SUM_NEGATIVE, batteries, Battery.ChannelId.CHARGE_MAX_CURRENT);
		this.calculate(INTEGER_SUM_NEGATIVE, batteries, Battery.ChannelId.DISCHARGE_MAX_CURRENT);

	}

	@VisibleForTesting
	static Integer calculateSoc(//
			List<Battery> batteries, //
			Map<Battery, Tuple2<Value<Integer>, Value<Integer>>> batteryValues //
	) {
		if (batteries.size() != batteryValues.size()) {
			return null;
		}

		if (!batteryValues.values().stream() //
				.allMatch(tuple -> tuple.a() != null && tuple.a().isDefined()//
						&& tuple.b() != null && tuple.b().isDefined())) {
			return null;
		}

		final var sumCapacity = batteryValues.values().stream() //
				.map(Tuple2::b) //
				.map(Value::get) //
				.mapToInt(value -> value) //
				.sum();

		return (int) batteryValues.values().stream() //
				.mapToDouble(tuple -> {
					final var socValue = tuple.a();
					final var capacityValue = tuple.b();

					return ((capacityValue.get() * 100d) / (sumCapacity * 100d)) * socValue.get();
				}) //
				.sum();
	}

	/**
	 * Aggregate Channels of {@link Battery}s.
	 *
	 * @param <T>        the Channel Type
	 * @param aggregator the aggregator function
	 * @param batteries  the List of {@link Battery}
	 * @param channelId  the Battery.ChannelId
	 */
	protected <T> void calculate(BinaryOperator<T> aggregator, List<Battery> batteries,
			io.openems.edge.common.channel.ChannelId channelId) {
		final Consumer<Value<T>> callback = (value) -> {

			final var result = batteries.stream() //
					.map(battery -> {
						final var channel = battery.<Channel<T>>channel(channelId);
						final var nextValue = channel.getNextValue();

						if (channel.channelDoc().getChannelCategory() == ChannelCategory.ENUM) {
							return nextValue.asEnum();
						}
						return nextValue.get();
					}) //
					.filter(Objects::nonNull) //
					.reduce(aggregator).orElse(null);

			var channel = this.parent.channel(channelId);
			channel.setNextValue(result);
		};
		for (var battery : batteries) {
			this.addOnSetNextValueListener(battery, channelId, callback);
		}
	}

}
