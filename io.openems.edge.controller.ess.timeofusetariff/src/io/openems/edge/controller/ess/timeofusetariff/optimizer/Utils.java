package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.common.type.TypeUtils.multiply;
import static io.openems.edge.common.type.TypeUtils.orElse;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController.PERIODS_PER_HOUR;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.util.Arrays.stream;
import static java.util.stream.IntStream.concat;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Streams;

import io.jenetics.util.RandomRegistry;
import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleResponse;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.ScheduleDatas;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.ScheduleDatas.ScheduleData;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.Period;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.timedata.api.Timedata;

/**
 * Utils for {@link TimeOfUseTariffController}.
 * 
 * <p>
 * All energy values are in [Wh] and positive, unless stated differently.
 */
public final class Utils {

	private Utils() {
	}

	/** Keep some buffer to avoid scheduling errors because of bad predictions. */
	public static final float ESS_MAX_SOC = 90F;

	/**
	 * C-Rate (capacity divided by time) during {@link StateMachine#CHARGE_GRID}.
	 * With a C-Rate of 0.5 the battery gets fully charged within 2 hours.
	 */
	public static final float ESS_CHARGE_C_RATE = 0.5F;

	public static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	public static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");
	public static final ChannelAddress SUM_GRID = new ChannelAddress("_sum", "GridActivePower");
	public static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress("_sum",
			"UnmanagedConsumptionActivePower");
	public static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress("_sum", "EssDischargePower");
	public static final ChannelAddress SUM_ESS_SOC = new ChannelAddress("_sum", "EssSoc");

	protected static final long EXECUTION_LIMIT_SECONDS_BUFFER = 30;
	protected static final long EXECUTION_LIMIT_SECONDS_MINIMUM = 60;

	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	/**
	 * Initializes the Jenetics {@link RandomRegistry} for production.
	 */
	public static void initializeRandomRegistryForProduction() {
		initializeRandomRegistry(false);
	}

	/**
	 * Initializes the Jenetics {@link RandomRegistry} for JUnit tests.
	 */
	public static void initializeRandomRegistryForUnitTest() {
		initializeRandomRegistry(true);
	}

	/**
	 * Initializes the Jenetics {@link RandomRegistry}.
	 * 
	 * <p>
	 * Default RandomGenerator "L64X256MixRandom" might not be available. Choose
	 * best available.
	 * 
	 * @param isUnitTest true for JUnit tests; false in production
	 */
	private static void initializeRandomRegistry(boolean isUnitTest) {
		System.setProperty("io.jenetics.util.defaultRandomGenerator", "Random");
		var rgf = RandomGeneratorFactory.all() //
				.filter(RandomGeneratorFactory::isStatistical) //
				.sorted((f, g) -> Integer.compare(g.stateBits(), f.stateBits())).findFirst()
				.orElse(RandomGeneratorFactory.of("Random"));
		if (isUnitTest) {
			RandomRegistry.random(rgf.create(315));
		} else {
			RandomRegistry.random(rgf.create());
		}
	}

	/**
	 * Create {@link Params} for {@link Simulator}.
	 * 
	 * @param context          the {@link Context} object
	 * @param existingSchedule the existing schedule, i.e. result of previous
	 *                         optimization
	 * @return {@link Params}
	 * @throws InvalidValueException on error
	 */
	public static Params createSimulatorParams(Context context,
			ImmutableSortedMap<ZonedDateTime, StateMachine> existingSchedule) throws InvalidValueException {
		final var time = roundDownToQuarter(ZonedDateTime.now());

		// Prediction values
		final var predictionConsumption = joinConsumptionPredictions(4, //
				context.predictorManager().getPrediction(SUM_CONSUMPTION).asArray(), //
				context.predictorManager().getPrediction(SUM_UNMANAGED_CONSUMPTION).asArray());
		final var predictionProduction = generateProductionPrediction(//
				context.predictorManager().getPrediction(SUM_PRODUCTION).asArray(), //
				predictionConsumption.length);

		// Prices contains the price values and the time it is retrieved.
		final var prices = context.timeOfUseTariff().getPrices();

		// Ess information.
		final var essTotalEnergy = context.ess().getCapacity().getOrError();
		final var essMinSocEnergy = getEssMinSocEnergy(context, essTotalEnergy);
		final var essMaxSocEnergy = round(ESS_MAX_SOC / 100F * essTotalEnergy);
		final var essSoc = context.ess().getSoc().getOrError();
		final var essSocEnergy = essTotalEnergy /* [Wh] */ / 100 * essSoc;

		// Power Values for scheduling battery for individual periods.
		var maxDischargePower = context.sum().getEssMaxDischargePower().orElse(1000 /* at least 1000 */);
		var maxChargePower = context.sum().getEssMaxDischargePower().orElse(-1000 /* at least 1000 */);

		return Params.create() //
				.setTime(time) //
				.setEssTotalEnergy(essTotalEnergy) //
				.setEssMinSocEnergy(essMinSocEnergy) //
				.setEssMaxSocEnergy(essMaxSocEnergy) //
				.setEssInitialEnergy(essSocEnergy) //
				.setEssMaxEnergy(toEnergy(min(maxDischargePower, abs(maxChargePower)))) //
				.seMaxBuyFromGrid(toEnergy(context.maxChargePowerFromGrid())) //
				.setProductions(stream(interpolateArray(predictionProduction)).map(v -> toEnergy(v)).toArray()) //
				.setConsumptions(stream(interpolateArray(predictionConsumption)).map(v -> toEnergy(v)).toArray()) //
				.setPrices(interpolateArray(prices.asArray())) //
				.setStates(context.controlMode().states) //
				.setExistingSchedule(existingSchedule) //
				.build();
	}

	/**
	 * Postprocesses production prediction; makes sure length is at least the same
	 * as consumption prediction - filling up with zeroes.
	 * 
	 * @param prediction the production prediction
	 * @param minLength  the min length (= consumption prediction length)
	 * @return new production prediction
	 */
	protected static Integer[] generateProductionPrediction(Integer[] prediction, int minLength) {
		if (prediction.length >= minLength) {
			return prediction;
		}
		return IntStream.range(0, minLength) //
				.mapToObj(i -> i > prediction.length - 1 ? 0 : prediction[i]) //
				.toArray(Integer[]::new);
	}

	protected static Integer[] joinConsumptionPredictions(int splitAfterIndex, Integer[] totalConsumption,
			Integer[] unmanagedConsumption) {
		return Streams.concat(//
				stream(totalConsumption) //
						.limit(splitAfterIndex), //
				stream(unmanagedConsumption) //
						.skip(splitAfterIndex)) //
				.toArray(Integer[]::new);
	}

	protected static boolean paramsAreValid(Params p) {
		if (p.optimizePeriods().isEmpty()) {
			// No periods are available
			LOG.warn("No periods are available");
			return false;
		}
		if (p.optimizePeriods().stream() //
				.allMatch(pp -> pp.production() == 0 && pp.consumption() == 0)) {
			// Production and Consumption predictions are all zero
			LOG.warn("Production and Consumption predictions are all zero");
			return false;
		}
		if (p.optimizePeriods().stream() //
				.mapToDouble(Params.OptimizePeriod::price) //
				.distinct() //
				.count() <= 1) {
			// Prices are all the same
			LOG.info("Prices are all the same");
			return false;
		}

		return true;
	}

	/**
	 * Returns the amount of energy that is not available for scheduling because of
	 * a configured minimum SoC.
	 * 
	 * @param context     the {@link Context}
	 * @param essCapacity net {@link SymmetricEss.ChannelId#CAPACITY}
	 * @return the value in [Wh]
	 */
	protected static int getEssMinSocEnergy(Context context, int essCapacity) {
		return essCapacity /* [Wh] */ / 100 //
				* getEssMinSocPercentage(//
						context.ctrlLimitTotalDischarges(), //
						context.ctrlEmergencyCapacityReserves());
	}

	/**
	 * Returns the configured minimum SoC, or zero.
	 * 
	 * @param ctrlLimitTotalDischarges      the list of
	 *                                      {@link ControllerEssLimitTotalDischarge}
	 * @param ctrlEmergencyCapacityReserves the list of
	 *                                      {@link ControllerEssEmergencyCapacityReserve}
	 * @return the value in [%]
	 */
	public static int getEssMinSocPercentage(List<ControllerEssLimitTotalDischarge> ctrlLimitTotalDischarges,
			List<ControllerEssEmergencyCapacityReserve> ctrlEmergencyCapacityReserves) {
		return concat(//
				ctrlLimitTotalDischarges.stream() //
						.map(ctrl -> ctrl.getMinSoc().get()) //
						.filter(Objects::nonNull) //
						.mapToInt(v -> max(0, v)), // only positives
				ctrlEmergencyCapacityReserves.stream() //
						.map(ctrl -> ctrl.getActualReserveSoc().get()) //
						.filter(Objects::nonNull) //
						.mapToInt(v -> max(0, v))) // only positives
				.max().orElse(0);
	}

	/**
	 * Interpolate an Array of {@link Double}s.
	 * 
	 * <p>
	 * Replaces nulls with previous value. If first entry is null, it is set to
	 * first available value. If all values are null, all are set to 0.
	 * 
	 * @param values the values
	 * @return values without nulls
	 */
	protected static double[] interpolateArray(Double[] values) {
		var firstNonNull = stream(values) //
				.filter(Objects::nonNull) //
				.findFirst();
		var lastNonNullIndex = IntStream.range(0, values.length) //
				.filter(i -> values[i] != null) //
				.reduce((first, second) -> second);
		if (lastNonNullIndex.isEmpty()) {
			return new double[0];
		}
		var result = new double[lastNonNullIndex.getAsInt() + 1];
		if (firstNonNull.isEmpty()) {
			// all null
			return result;
		}
		double last = firstNonNull.get();
		for (var i = 0; i < result.length; i++) {
			double value = orElse(values[i], last);
			result[i] = last = value;
		}
		return result;
	}

	/**
	 * Interpolate an Array of {@link Integer}s.
	 * 
	 * <p>
	 * Replaces nulls with previous value. If first entry is null, it is set to
	 * first available value. If all values are null, all are set to 0.
	 * 
	 * @param values the values
	 * @return values without nulls
	 */
	protected static int[] interpolateArray(Integer[] values) {
		var firstNonNull = stream(values) //
				.filter(Objects::nonNull) //
				.findFirst();
		var lastNonNullIndex = IntStream.range(0, values.length) //
				.filter(i -> values[i] != null) //
				.reduce((first, second) -> second); //
		if (lastNonNullIndex.isEmpty()) {
			return new int[0];
		}
		var result = new int[lastNonNullIndex.getAsInt() + 1];
		if (firstNonNull.isEmpty()) {
			// all null
			return result;
		}
		int last = firstNonNull.get();
		for (var i = 0; i < result.length; i++) {
			int value = orElse(values[i], last);
			result[i] = last = value;
		}
		return result;
	}

	protected static int findFirstPeakIndex(int fromIndex, double[] values) {
		if (values.length <= fromIndex) {
			return fromIndex;
		} else {
			var previous = values[fromIndex];
			for (var i = fromIndex + 1; i < values.length; i++) {
				var value = values[i];
				if (value < previous) {
					return i - 1;
				}
				previous = value;
			}
		}
		return values.length - 1;
	}

	protected static int findFirstValleyIndex(int fromIndex, double[] values) {
		if (values.length <= fromIndex) {
			return fromIndex;
		} else {
			var previous = values[fromIndex];
			for (var i = fromIndex + 1; i < values.length; i++) {
				var value = values[i];
				if (value > previous) {
					return i - 1;
				}
				previous = value;
			}
		}
		return values.length - 1;
	}

	/**
	 * Utilizes the previous three hours' data and computes the next 21 hours data
	 * from the {@link Optimizer} provided, then concatenates them to generate a
	 * 24-hour {@link GetScheduleResponse}.
	 * 
	 * @param optimizer   the {@link Optimizer}
	 * @param requestId   the JSON-RPC request-id
	 * @param timedata    the{@link Timedata}
	 * @param componentId the Component-ID
	 * @param now         the current {@link ZonedDateTime} (will get rounded down
	 *                    to 15 minutes)
	 * @return the {@link GetScheduleResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static GetScheduleResponse handleGetScheduleRequest(Optimizer optimizer, UUID requestId, Timedata timedata,
			String componentId, ZonedDateTime now) throws OpenemsNamedException {
		now = roundDownToQuarter(now);

		final var schedule = optimizer.getSchedule();
		if (schedule == null) {
			throw new OpenemsException("Has no Schedule");
		}
		final var params = optimizer.getParams();
		if (params == null) {
			throw new OpenemsException("Has no Params");
		}
		final var channelQuarterlyPrices = new ChannelAddress(componentId, "QuarterlyPrices");
		final var channelStateMachine = new ChannelAddress(componentId, "StateMachine");
		final var b = ImmutableList.<ScheduleData>builder();

		// Process past data
		final var fromDate = now.minusHours(3);
		try {
			var queryResult = timedata.queryHistoricData(null, fromDate, now, //
					Set.of(channelQuarterlyPrices, channelStateMachine, //
							SUM_GRID, SUM_PRODUCTION, SUM_CONSUMPTION, SUM_ESS_DISCHARGE_POWER, SUM_ESS_SOC),
					new Resolution(15, ChronoUnit.MINUTES));
			ScheduleData.fromHistoricDataQuery(//
					fromDate, params.essTotalEnergy(), channelQuarterlyPrices, channelStateMachine, queryResult) //
					.forEach(b::add);
		} catch (Exception e) {
			LOG.warn("Unable to read historic data: " + e.getMessage());
		}

		// Process future schedule
		optimizer.getSchedule().values().stream() //
				.flatMap(ScheduleData::fromPeriod) //
				.forEach(b::add);

		return new GetScheduleResponse(requestId, fromDate, new ScheduleDatas(params.essTotalEnergy(), b.build()));
	}

	/**
	 * Calculates the ExecutionLimitSeconds for the {@link Optimizer}.
	 * 
	 * @param clock a clock
	 * @return execution limit in [s]
	 */
	public static long calculateExecutionLimitSeconds(Clock clock) {
		var now = ZonedDateTime.now(clock);
		var nextQuarter = roundDownToQuarter(now).plusMinutes(15).minusSeconds(EXECUTION_LIMIT_SECONDS_BUFFER);
		var duration = Duration.between(now, nextQuarter).getSeconds();
		if (duration >= EXECUTION_LIMIT_SECONDS_MINIMUM) {
			return duration;
		}
		// Otherwise add 15 more minutes
		return Duration.between(now, nextQuarter.plusMinutes(15)).getSeconds();
	}

	/**
	 * Post-Process a state of a Period during Simulation, i.e. replace with
	 * 'better' state with the same behaviour.
	 * 
	 * <p>
	 * NOTE: heavy computation is ok here, because this method is called only at the
	 * end with the best Schedule.
	 * 
	 * @param p                the {@link Params}
	 * @param essInitialEnergy the initial ESS energy in this period
	 * @param state            the initial state
	 * @param ef               the {@link EnergyFlow}
	 * @return the new state
	 */
	public static StateMachine postprocessSimulatorState(Params p, int essInitialEnergy, StateMachine state,
			EnergyFlow ef) {
		return switch (state) {
		case BALANCING -> state;

		case DELAY_DISCHARGE -> {
			// DELAY_DISCHARGE,...
			if (essInitialEnergy <= p.essMinSocEnergy()) {
				// but battery is already empty (at Min-Soc)
				yield BALANCING;
			} else if (ef.productionToEss() > 0) {
				// but actually charging -> could have been BALANCING
				yield BALANCING;
			}
			yield state;
		}

		case CHARGE_GRID -> {
			// CHARGE_GRID,...
			if (ef.gridToEss() == 0) {
				// but actually not charging
				yield BALANCING;
			} else if (essInitialEnergy > p.essMaxSocEnergy()) {
				// but battery is above limit
				yield DELAY_DISCHARGE;
			}
			yield state;
		}
		};
	}

	/**
	 * Post-Process a state during {@link Controller#run()}, i.e. replace with
	 * 'better' state if appropriate.
	 * 
	 * <p>
	 * NOTE: this can be useful, if live operation deviates from predicted
	 * operation, e.g. because predictions were wrong.
	 * 
	 * @param minSoc     the configured Minimum-SoC, or zero
	 * @param soc        the current {@link SymmetricEss.ChannelId#SOC}
	 * @param production the current {@link Sum.ChannelId#PRODUCTION_ACTIVE_POWER},
	 *                   or zero
	 * @param state      the initial state
	 * @return the new state
	 */
	public static StateMachine postprocessRunState(int minSoc, Integer soc, int production, StateMachine state) {
		if (soc == null) {
			return state;
		}

		return switch (state) {
		case BALANCING -> state;

		case DELAY_DISCHARGE -> {
			// DELAY_DISCHARGE,...
			if (soc <= minSoc) {
				// but SoC is at Min-SoC -> could have been BALANCING
				yield BALANCING;
			}
			yield state;
		}

		case CHARGE_GRID -> {
			// CHARGE_GRID,...
			if (soc > ESS_MAX_SOC) {
				// but surpassed Max-SoC -> stop charge; no discharge
				yield DELAY_DISCHARGE;
			}
			yield state;
		}
		};
	}

	protected static int calculateEssChargeInChargeGridPowerFromParams(Params params, ManagedSymmetricEss ess) {
		if (params != null) {
			for (var period : params.optimizePeriods()) {
				return toPower(period.essChargeInChargeGrid()); // take first period
			}
		}
		var capacity = ess.getCapacity();
		if (capacity.isDefined()) {
			return round(capacity.get() * ESS_CHARGE_C_RATE);
		}
		var maxApparentPower = ess.getMaxApparentPower();
		if (maxApparentPower.isDefined()) {
			return maxApparentPower.get() / 4;
		}
		return 0;
	}

	/**
	 * Calculates the Max-ActivePower constraint for
	 * {@link StateMachine#CHARGE_GRID}.
	 * 
	 * @param params                 the {@link Params}
	 * @param ess                    the {@link ManagedSymmetricEss}
	 * @param sum                    the {@link Sum}
	 * @param maxChargePowerFromGrid the configured max charge from grid power
	 * @return the set-point or null
	 */
	public static Integer calculateChargeGridPower(Params params, ManagedSymmetricEss ess, Sum sum,
			int maxChargePowerFromGrid) {
		var gridActivePower = sum.getGridActivePower().get(); // current buy-from/sell-to grid
		var essActivePower = ess.getActivePower().get(); // current charge/discharge ESS
		if (gridActivePower == null || essActivePower == null) {
			return null; // undefined state
		}

		var realGridPower = gridActivePower + essActivePower; // 'real', without current ESS charge/discharge
		var targetChargePower = calculateEssChargeInChargeGridPowerFromParams(params, ess) //
				+ min(0, realGridPower) * -1; // add excess production
		var effectiveGridBuyPower = max(0, realGridPower) + targetChargePower;
		var chargePower = max(0, targetChargePower - max(0, effectiveGridBuyPower - maxChargePowerFromGrid));

		// Invert to negative for CHARGE
		chargePower *= -1;

		// Apply §14a EnWG limit
		if (limitChargePowerFor14aEnWG) {
			chargePower = max(ESS_LIMIT_14A_ENWG, chargePower);
		}

		return chargePower;
	}

	/**
	 * Calculates the Max-ActivePower constraint for
	 * {@link StateMachine#CHARGE_PRODUCTION}.
	 * 
	 * @param sum the {@link Sum}
	 * @return the set-point
	 */
	public static Integer calculateMaxChargeProductionPower(Sum sum) {
		var productionAcActivePower = sum.getProductionAcActivePower().get();
		if (productionAcActivePower == null || productionAcActivePower < 0) {
			return 0; // unknown AC production -> do not charge
		}
		return -productionAcActivePower;
	}

	/**
	 * Calculates the ActivePower constraint for
	 * {@link StateMachine#DELAY_DISCHARGE}.
	 * 
	 * @param ess the {@link ManagedSymmetricEss}
	 * @return the set-point
	 */
	public static Integer calculateDelayDischargePower(ManagedSymmetricEss ess) {
		if (ess instanceof HybridEss) {
			// Limit discharge to DC-PV power
			return max(0, ess.getActivePower().orElse(0) - ((HybridEss) ess).getDcDischargePower().orElse(0));
		} else {
			// Limit discharge to 0
			return 0;
		}
	}

	/**
	 * Converts power [W] to energy [Wh/15 min].
	 * 
	 * @param power the power value
	 * @return the energy value
	 */
	public static int toEnergy(int power) {
		return power / PERIODS_PER_HOUR;
	}

	/**
	 * Converts energy [Wh/15 min] to power [W].
	 * 
	 * @param energy the energy value
	 * @return the power value
	 */
	public static Integer toPower(Integer energy) {
		return multiply(energy, PERIODS_PER_HOUR);
	}

	/**
	 * Prints the Schedule to System.out.
	 * 
	 * <p>
	 * NOTE: The output format is suitable as input for "RunOptimizerFromLogApp".
	 * This is useful to re-run a simulation.
	 * 
	 * @param params  the {@link Params}
	 * @param periods the map of {@link Period}s
	 */
	protected static void logSchedule(Params params, ImmutableSortedMap<ZonedDateTime, Period> periods) {
		System.out.println("OPTIMIZER " + params.toLogString());
		System.out.println(ScheduleDatas.fromSchedule(params.essTotalEnergy(), periods).toLogString("OPTIMIZER "));
	}

	/**
	 * Updates the active Schedule with a new Schedule.
	 * 
	 * <p>
	 * <ul>
	 * <li>Period of the currently active Quarter is never changed
	 * <li>Old Periods are removed from the Schedule
	 * <li>Remaining Schedules are updated from new Schedule
	 * </ul>
	 * 
	 * @param now         the current {@link ZonedDateTime}
	 * @param schedule    the active Schedule
	 * @param newSchedule the new Schedule
	 */
	public static void updateSchedule(ZonedDateTime now, TreeMap<ZonedDateTime, Period> schedule,
			ImmutableSortedMap<ZonedDateTime, Period> newSchedule) {
		var thisQuarter = roundDownToQuarter(now);
		var current = schedule.get(thisQuarter);
		schedule.clear();
		schedule.putAll(newSchedule);
		if (current != null) {
			schedule.put(thisQuarter, current);
		}
	}
}
