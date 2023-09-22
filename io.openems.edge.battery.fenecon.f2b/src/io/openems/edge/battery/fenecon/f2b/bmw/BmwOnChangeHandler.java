package io.openems.edge.battery.fenecon.f2b.bmw;

import static io.openems.edge.battery.fenecon.f2b.cluster.common.Constants.DEFAULT_MIN_CELL_TARGET_VOLTAGE;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingConsumer;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.battery.fenecon.f2b.DeviceSpecificOnChangeHandler;
import io.openems.edge.battery.fenecon.f2b.cluster.common.BatteryFeneconF2bCluster;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.type.TypeUtils;

public class BmwOnChangeHandler implements DeviceSpecificOnChangeHandler<BatteryFeneconF2bBmw> {

	@Override
	public Class<BatteryFeneconF2bBmw> getBatteryType() {
		return BatteryFeneconF2bBmw.class;
	}

	@Override
	public List<OnChangeCallback> getOnChangeCallbacks() {
		return List.of(//
				new OnChangeCallback(BmwOnChangeHandler::requestBalancing,
						List.of(BatteryFeneconF2bBmw.ChannelId.BALANCING_CONDITION, //
								BatteryFeneconF2bBmw.ChannelId.OCV_REACHED, //
								BatteryFeneconF2bBmw.ChannelId.BALANCING_STILL_RUNNING, //
								BatteryFeneconF2bBmw.ChannelId.BALANCING_MIN_CELL_VOLTAGE)), //
				new OnChangeCallback(BmwOnChangeHandler::requestHeating,
						List.of(BatteryFeneconF2b.ChannelId.AVG_CELL_TEMPERATURE)), //
				new OnChangeCallback(BmwOnChangeHandler::updateBmwClusterVoltage,
						List.of(BatteryFeneconF2b.ChannelId.INTERNAL_VOLTAGE))) //
		;
	}

	/**
	 * The objective of balancing is to adapt the voltages of all the cells to the
	 * same level in the battery. This is necessary in case of an asymmetry, which
	 * means that the maximum usable electric capacity of the cell is limited
	 * because of a voltage restriction of other cells during charging and
	 * discharging process and therefore cannot be used completely.
	 * 
	 * <p>
	 * The BMW battery has a passive balancing mechanism. Passive balancing means a
	 * load resistance can be turned on individually in series to every cell to
	 * reduce the voltage level.
	 * </p>
	 * 
	 * 
	 * <p>
	 * If the batteries are connected electrically in series to one another, then it
	 * makes sense to balance all cells in the 2 batteries to a common cell voltage
	 * level.
	 * </p>
	 * 
	 * @param batteries list of {@link BatteryFeneconF2b}.
	 * @param cluster   {@link BatteryFeneconF2bCluster}.
	 */
	public static void requestBalancing(List<BatteryFeneconF2b> batteries, BatteryFeneconF2bCluster cluster) {
		if (batteries.isEmpty()) {
			return;
		}
		var balancableBatteries = batteries.stream()//
				.filter(BatteryFeneconF2bBmw.class::isInstance) //
				.map(BatteryFeneconF2bBmw.class::cast) //
				.toList();

		if (!areChannelsDefined(batteries)) {
			return;
		}

		if (!cluster.areAllBatteriesStarted()) {
			BmwOnChangeHandler.setDefaultBalancingValues(balancableBatteries);
			return;
		}

		var cellAvgtemp = batteries.stream()//
				.map(BatteryFeneconF2b::getAvgCellTemperature)//
				.filter(Value::isDefined)//
				.mapToInt(Value::get)//
				.reduce(TypeUtils::averageInt)//
				.getAsInt();
		var minCellVoltage = cluster.getMinCellVoltage().get();
		var maxCellVoltage = cluster.getMaxCellVoltage().get();
		var soc = cluster.getSoc().get();

		if ((minCellVoltage < 3000) // [mV]
				|| (maxCellVoltage > 4000) // [mV]
				|| (soc < 30)// [%]
				|| (cellAvgtemp < 0) || (cellAvgtemp > 40)) {
			BmwOnChangeHandler.setDefaultBalancingValues(balancableBatteries);
			return;
		}

		var balancingMinCellVoltage = calculate(null, INTEGER_MIN, balancableBatteries,
				BatteryFeneconF2bBmw.ChannelId.BALANCING_MIN_CELL_VOLTAGE);
		var balancingCondition = calculate(true, Boolean::logicalAnd, balancableBatteries,
				BatteryFeneconF2bBmw.ChannelId.BALANCING_CONDITION);
		var ocvReached = calculate(true, Boolean::logicalAnd, balancableBatteries,
				BatteryFeneconF2bBmw.ChannelId.OCV_REACHED);
		var balancingStillRunning = calculate(false, Boolean::logicalOr, balancableBatteries,
				BatteryFeneconF2bBmw.ChannelId.BALANCING_STILL_RUNNING);

		balancableBatteries.forEach(b -> {
			try {
				b.setBalancingConditionsFullfilled(balancingCondition ? 2 : 1);
				b.setOcvReachedAtAllTheBatteries(ocvReached ? 2 : 1);
				b.setBalancingRunning(balancingStillRunning ? 2 : 1);
				b.setBalancingTargetVoltage(
						balancingCondition ? balancingMinCellVoltage : DEFAULT_MIN_CELL_TARGET_VOLTAGE);
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
		});
	}

	private static void setDefaultBalancingValues(List<BatteryFeneconF2bBmw> batteries) {
		batteries.forEach(b -> {
			try {
				b.setBalancingConditionsFullfilled(1);
				b.setOcvReachedAtAllTheBatteries(1);
				b.setBalancingRunning(1);
				b.setBalancingTargetVoltage(DEFAULT_MIN_CELL_TARGET_VOLTAGE);
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
		});
	}

	private static final BiFunction<Integer, Integer, Integer> INTEGER_MIN = TypeUtils::min;

	/**
	 * Aggregate Channels .
	 *
	 * @param <T>       the Channel Type
	 * @param init      the initial value
	 * @param method    the aggregator function
	 * @param batteries the List of {@link BatteryBalancable}
	 * @param channelId the BatteryBalancable.ChannelId
	 * @return T generic type according to the result.
	 */
	private static <T> T calculate(//
			final T init, //
			final BiFunction<T, T, T> method, //
			final List<BatteryFeneconF2bBmw> batteries, //
			final BatteryFeneconF2bBmw.ChannelId channelId //
	) {
		T result = init;
		for (var battery : batteries) {
			Channel<T> channel = battery.channel(channelId);
			var channelValue = channel.value();
			if (!channelValue.isDefined()) {
				continue;
			}
			result = method.apply(result, channelValue.get());
		}
		return result;
	}

	private static boolean areChannelsDefined(List<BatteryFeneconF2b> batteries) {
		return batteries.stream()//
				.filter(BatteryFeneconF2bBmw.class::isInstance)//
				.map(BatteryFeneconF2bBmw.class::cast)//
				.allMatch(t -> {
					return Stream.of(t.getBalancingCondition(), //
							t.getBalancingMinCellVoltage(), //
							t.getOcvReached(), //
							t.getBalancingStillRunning(), //
							t.getMinCellVoltage(), //
							t.getAvgCellTemperature(), //
							t.getSoc(), //
							t.getMaxCellVoltage())//
							.allMatch(Value::isDefined);//
				});
	}

	/**
	 * The cells can be heated up through the dissipated power from the current
	 * flowing through an electrical wire inside the {@link Battery}. The current
	 * flow through the electrical wire is activated and deactivated by a MOSFET,
	 * which is controlled by the battery management system.
	 * 
	 * <p>
	 * In order to request heat for a serial connected batteries, serial cluster
	 * must decide whether all battery temperatures are less than 10
	 * {@link Unit#DEGREE_CELSIUS}
	 * </p>
	 * 
	 * 
	 * @param batteries the List of {@link Battery}
	 * @param cluster   {@link BatteryFeneconF2bCluster}
	 */
	public static void requestHeating(List<BatteryFeneconF2b> batteries, BatteryFeneconF2bCluster cluster) {
		if (batteries.isEmpty()) {
			return;
		}
		ThrowingConsumer<BatteryFeneconF2bBmw, OpenemsNamedException> method = null;
		if (batteries.stream()//
				.allMatch(battery -> battery.getAvgCellTemperature().isDefined()//
						&& battery.getAvgCellTemperature().get() <= 10)//
				&& cluster.areAllBatteriesStarted()) {
			method = BatteryFeneconF2bBmw::startHeating;
		} else {
			method = BatteryFeneconF2bBmw::stopHeating;
		}

		for (var battery : batteries) {
			try {
				method.accept(((BatteryFeneconF2bBmw) battery));
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sets the cluster voltage based on the summation of link voltages before
	 * batteries started.
	 * 
	 * @param batteries the batteries
	 * @param cluster   the battery cluster
	 */
	public static void updateBmwClusterVoltage(List<BatteryFeneconF2b> batteries, BatteryFeneconF2bCluster cluster) {
		var batteryList = batteries.stream()//
				.filter(BatteryFeneconF2bBmw.class::isInstance) //
				.map(BatteryFeneconF2bBmw.class::cast) //
				.toList();
		if (cluster.areAllBatteriesStopped()) {
			cluster._setInternalVoltage(null);
			return;
		}
		Integer batteryVoltage = null;
		for (var battery : batteryList) {
			if (!battery.getInternalVoltage().isDefined()) {
				return;
			}
			batteryVoltage = TypeUtils.sum(batteryVoltage, battery.getInternalVoltage().get());
		}
		cluster._setInternalVoltage(TypeUtils.divide(batteryVoltage, 10));
	}
}
