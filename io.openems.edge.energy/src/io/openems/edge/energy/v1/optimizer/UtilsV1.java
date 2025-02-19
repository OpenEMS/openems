package io.openems.edge.energy.v1.optimizer;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.common.type.TypeUtils.multiply;
import static io.openems.edge.common.type.TypeUtils.orElse;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.Utils.ESS_MAX_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.v1.UtilsV1.calculateLimitChargePowerFor14aEnWG;
import static io.openems.edge.controller.ess.timeofusetariff.v1.UtilsV1.getEssMinSocPercentage;
import static io.openems.edge.energy.api.EnergyConstants.PERIODS_PER_HOUR;
import static io.openems.edge.energy.optimizer.Utils.SUM_ESS_DISCHARGE_POWER;
import static io.openems.edge.energy.optimizer.Utils.SUM_ESS_SOC;
import static io.openems.edge.energy.optimizer.Utils.SUM_GRID;
import static java.lang.Math.round;
import static java.util.Arrays.stream;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Streams;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.Context;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController;
import io.openems.edge.controller.ess.timeofusetariff.v1.EnergyScheduleHandlerV1.ContextV1;
import io.openems.edge.energy.v1.jsonrpc.GetScheduleResponse;
import io.openems.edge.energy.v1.optimizer.ScheduleDatas.ScheduleData;
import io.openems.edge.energy.v1.optimizer.SimulatorV1.Period;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

/**
 * Utils for {@link TimeOfUseTariffController}.
 * 
 * <p>
 * All energy values are in [Wh] and positive, unless stated differently.
 */
@Deprecated
public final class UtilsV1 {

	private UtilsV1() {
	}

	public static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	public static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");
	public static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress("_sum",
			"UnmanagedConsumptionActivePower");

	private static final Logger LOG = LoggerFactory.getLogger(UtilsV1.class);

	/**
	 * Create {@link ParamsV1} for {@link SimulatorV1}.
	 * 
	 * @param globalContext    the {@link GlobalContextV1} object
	 * @param existingSchedule the existing schedule, i.e. result of previous
	 *                         optimization
	 * @return {@link ParamsV1}
	 * @throws InvalidValueException on error
	 */
	public static ParamsV1 createSimulatorParams(GlobalContextV1 globalContext,
			ImmutableSortedMap<ZonedDateTime, StateMachine> existingSchedule) throws InvalidValueException {
		final var time = roundDownToQuarter(ZonedDateTime.now());

		// Prediction values
		final var predictionConsumption = joinConsumptionPredictions(4, //
				globalContext.predictorManager().getPrediction(SUM_CONSUMPTION).asArray(), //
				globalContext.predictorManager().getPrediction(SUM_UNMANAGED_CONSUMPTION).asArray());
		final var predictionProduction = generateProductionPrediction(//
				globalContext.predictorManager().getPrediction(SUM_PRODUCTION).asArray(), //
				predictionConsumption.length);

		// Prices contains the price values and the time it is retrieved.
		final var prices = globalContext.timeOfUseTariff().getPrices();

		// Ess information.
		var context = globalContext.energyScheduleHandler().getContext();
		final var essTotalEnergy = context.ess().getCapacity().getOrError();
		final var essMinSocEnergy = getEssMinSocEnergy(context, essTotalEnergy);
		final var essMaxSocEnergy = round(ESS_MAX_SOC / 100F * essTotalEnergy);
		final var essSoc = context.ess().getSoc().getOrError();
		final var essSocEnergy = essTotalEnergy /* [Wh] */ / 100 * essSoc;

		// Power Values for scheduling battery for individual periods.
		var maxDischargePower = globalContext.sum().getEssMaxDischargePower().orElse(1000 /* at least 1000 */);
		var maxChargePower = calculateMaxChargePower(//
				calculateLimitChargePowerFor14aEnWG(context.ctrlLimiter14as()), // Apply §14a EnWG limit
				globalContext.sum().getEssMaxDischargePower(), -1000 /* at least 1000 */);

		return ParamsV1.create() //
				.setTime(time) //
				.setEssTotalEnergy(essTotalEnergy) //
				.setEssMinSocEnergy(essMinSocEnergy) //
				.setEssMaxSocEnergy(essMaxSocEnergy) //
				.setEssInitialEnergy(essSocEnergy) //
				.setEssMaxChargeEnergy(toEnergy(maxChargePower)) //
				.setEssMaxDischargeEnergy(toEnergy(maxDischargePower)) //
				.seMaxBuyFromGrid(toEnergy(context.maxChargePowerFromGrid())) //
				.setProductions(stream(interpolateArray(predictionProduction)).map(v -> toEnergy(v)).toArray()) //
				.setConsumptions(stream(interpolateArray(predictionConsumption)).map(v -> toEnergy(v)).toArray()) //
				.setPrices(interpolateDoubleArray(prices.asArray())) //
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

	protected static boolean paramsAreValid(ParamsV1 p) {
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
				.mapToDouble(ParamsV1.OptimizePeriod::price) //
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
	protected static int getEssMinSocEnergy(ContextV1 context, int essCapacity) {
		return essCapacity /* [Wh] */ / 100 //
				* getEssMinSocPercentage(//
						context.ctrlLimitTotalDischarges(), //
						context.ctrlEmergencyCapacityReserves());
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
	protected static double[] interpolateDoubleArray(Double[] values) {
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
	 * Utilizes the previous three hours' data and computes the next 21 hours data
	 * from the {@link OptimizerV1} provided, then concatenates them to generate a
	 * 24-hour {@link GetScheduleResponse}.
	 * 
	 * @param optimizer       the {@link OptimizerV1}
	 * @param requestId       the JSON-RPC request-id
	 * @param timedata        the{@link Timedata}
	 * @param timeOfUseTariff the {@link TimeOfUseTariff}
	 * @param componentId     the Component-ID
	 * @param now             the current {@link ZonedDateTime} (will get rounded
	 *                        down to 15 minutes)
	 * @return the {@link GetScheduleResponse}
	 */
	public static GetScheduleResponse handleGetScheduleRequest(OptimizerV1 optimizer, UUID requestId, Timedata timedata,
			TimeOfUseTariff timeOfUseTariff, String componentId, ZonedDateTime now) {
		final var b = ImmutableList.<ScheduleData>builder();
		now = roundDownToQuarter(now);
		final var fromTime = now.minusHours(3);

		final var params = optimizer.getParams();
		if (params != null) {
			// Process last three hours of historic data
			final var channelQuarterlyPrices = new ChannelAddress(componentId, "QuarterlyPrices");
			final var channelStateMachine = new ChannelAddress(componentId, "StateMachine");
			try {
				var queryResult = timedata.queryHistoricData(null, fromTime, now, //
						Set.of(channelQuarterlyPrices, channelStateMachine, //
								SUM_GRID, SUM_PRODUCTION, SUM_CONSUMPTION, SUM_ESS_DISCHARGE_POWER, SUM_ESS_SOC),
						new Resolution(15, ChronoUnit.MINUTES));
				ScheduleData.fromHistoricDataQuery(//
						params.essTotalEnergy(), channelQuarterlyPrices, channelStateMachine, queryResult) //
						.forEach(b::add);
			} catch (Exception e) {
				LOG.warn("Unable to read historic data: " + e.getMessage());
			}
		}

		// Process future schedule
		final var schedule = optimizer.getSchedule();
		optimizer.getSchedule().values().stream() //
				.flatMap(ScheduleData::fromPeriod) //
				.forEach(b::add);

		// Find 'toTime' of result
		final ZonedDateTime toTime;
		if (!schedule.isEmpty()) {
			toTime = schedule.lastKey();
		} else {
			var lastTime = timeOfUseTariff.getPrices().getLastTime();
			if (lastTime != null) {
				toTime = lastTime;
			} else {
				toTime = fromTime;
			}
		}

		return new GetScheduleResponse(requestId, fromTime, toTime,
				new ScheduleDatas(params.essTotalEnergy(), b.build()));
	}

	/**
	 * Post-Process a state of a Period during Simulation, i.e. replace with
	 * 'better' state with the same behaviour.
	 * 
	 * <p>
	 * NOTE: heavy computation is ok here, because this method is called only at the
	 * end with the best Schedule.
	 * 
	 * @param state            the initial state
	 * @param efBalancing      the {@link EnergyFlowV1} as it would be in
	 *                         {@link StateMachine#BALANCING}
	 * @param efDelayDischarge the {@link EnergyFlowV1} as it would be in
	 *                         {@link StateMachine#DELAY_DISCHARGE}
	 * @param efChargeGrid     the {@link EnergyFlowV1} as it would be in
	 *                         {@link StateMachine#CHARGE_GRID}
	 * @return the new state
	 */
	public static StateMachine postprocessSimulatorState(StateMachine state, EnergyFlowV1 efBalancing,
			EnergyFlowV1 efDelayDischarge, EnergyFlowV1 efChargeGrid) {
		if (state == CHARGE_GRID) {
			// CHARGE_GRID,...
			if (efChargeGrid.ess() >= efDelayDischarge.ess()) {
				// but battery charge/discharge is the same as DELAY_DISCHARGE
				state = DELAY_DISCHARGE;
			}
		}

		if (state == DELAY_DISCHARGE) {
			// DELAY_DISCHARGE,...
			if (efDelayDischarge.ess() >= efBalancing.ess()) {
				// but battery charge/discharge is the same as BALANCING
				state = BALANCING;
			}
		}

		return state;
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
	 * @param params  the {@link ParamsV1}
	 * @param periods the map of {@link Period}s
	 */
	protected static void logSchedule(ParamsV1 params, ImmutableSortedMap<ZonedDateTime, Period> periods) {
		System.out.println("OPTIMIZER " + params.toLogString());
		System.out.println(ScheduleDatas.fromSchedule(params.essTotalEnergy(), periods).toLogString("OPTIMIZER "));
	}

	/**
	 * Updates the active Schedule with a new Schedule.
	 * 
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

	/**
	 * Calculates the max-charge-power.
	 * 
	 * @param calculateLimitChargePowerFor14aEnWG negative value provided by
	 *                                            calculateLimitChargePowerFor14aEnWG;
	 *                                            possilby Integer.MIN_VALUE
	 * @param essMaxDischargePower                {@link Sum.ChannelId#ESS_MAX_DISCHARGE_POWER};
	 *                                            positive
	 * @param orElse                              a default value; negative
	 * @return max-charge-power as positive value
	 */
	protected static int calculateMaxChargePower(int calculateLimitChargePowerFor14aEnWG,
			Value<Integer> essMaxDischargePower, int orElse) {
		return Math.abs(//
				Math.max(//
						calculateLimitChargePowerFor14aEnWG, // Apply §14a EnWG limit
						-1 * essMaxDischargePower.orElse(orElse)));
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
	public static int[] interpolateArray(Integer[] values) {
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
}
