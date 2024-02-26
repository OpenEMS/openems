package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.JsonUtils.getAsOptionalDouble;
import static io.openems.common.utils.JsonUtils.getAsOptionalInt;
import static io.openems.common.utils.JsonUtils.toJson;
import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static io.openems.edge.common.type.TypeUtils.multiply;
import static io.openems.edge.common.type.TypeUtils.orElse;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController.PERIODS_PER_HOUR;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Arrays.stream;
import static java.util.stream.IntStream.concat;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischarge;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleResponse;
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

	protected static final ChannelAddress SUM_GRID = new ChannelAddress("_sum", "GridActivePower");
	protected static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	protected static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress("_sum",
			"UnmanagedConsumptionActivePower");
	protected static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");
	protected static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress("_sum", "EssDischargePower");
	protected static final ChannelAddress SUM_ESS_SOC = new ChannelAddress("_sum", "EssSoc");

	protected static final long EXECUTION_LIMIT_SECONDS_BUFFER = 30;
	protected static final long EXECUTION_LIMIT_SECONDS_MINIMUM = 60;

	public record ScheduleData(Double quarterlyPrice, Integer stateMachine, Integer grid, Integer production,
			Integer consumption, Integer ess, Integer soc) {
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
	public static Params createSimulatorParams(Context context, TreeMap<ZonedDateTime, Period> existingSchedule)
			throws InvalidValueException {
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
				.time(time) //
				.essTotalEnergy(essTotalEnergy) //
				.essMinSocEnergy(essMinSocEnergy) //
				.essMaxSocEnergy(essMaxSocEnergy) //
				.essInitialEnergy(essSocEnergy) //
				.essMaxEnergyPerPeriod(toEnergy(min(maxDischargePower, abs(maxChargePower)))) //
				.maxBuyFromGrid(toEnergy(context.maxChargePowerFromGrid())) //
				.productions(stream(interpolateArray(predictionProduction)).map(v -> toEnergy(v)).toArray()) //
				.consumptions(stream(interpolateArray(predictionConsumption)).map(v -> toEnergy(v)).toArray()) //
				.prices(interpolateArray(prices.asArray())) //
				.states(context.controlMode().states) //
				.existingSchedule(existingSchedule) //
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
				Arrays.stream(totalConsumption) //
						.limit(splitAfterIndex), //
				Arrays.stream(unmanagedConsumption) //
						.skip(splitAfterIndex)) //
				.toArray(Integer[]::new);
	}

	/**
	 * Builds an initial population:
	 * 
	 * <ol>
	 * <li>Schedule with all periods BALANCING
	 * <li>Schedule from currently existing Schedule, i.e. the bestGenotype of last
	 * optimization run
	 * </ol>
	 * 
	 * <p>
	 * NOTE: providing an "all periods BALANCING" Schedule as first Genotype makes
	 * sure, that this one wins in case there are other results with same cost, e.g.
	 * when battery never gets empty anyway.
	 * 
	 * @param p the {@link Params}
	 * @return the {@link Genotype}
	 */
	public static ImmutableList<Genotype<IntegerGene>> buildInitialPopulation(Params p) {
		var states = List.of(p.states());
		var b = ImmutableList.<Genotype<IntegerGene>>builder() //
				// All BALANCING
				.add(Genotype.of(//
						IntStream.range(0, p.numberOfPeriods()) //
								.map(i -> states.indexOf(BALANCING)) //
								.mapToObj(state -> IntegerChromosome.of(IntegerGene.of(state, 0, p.states().length))) //
								.toList()));

		if (p.existingSchedule().length > 0 //
				&& Stream.of(p.existingSchedule()) //
						.anyMatch(s -> s != StateMachine.BALANCING)) {
			// Existing Schedule if available
			b.add(Genotype.of(//
					IntStream.range(0, p.numberOfPeriods()) //
							// Map to state index; not-found maps to '-1', corrected to '0'
							.map(i -> fitWithin(0, p.states().length, states.indexOf(//
									p.existingSchedule().length > i //
											? p.existingSchedule()[i] //
											: BALANCING))) //
							.mapToObj(state -> IntegerChromosome.of(IntegerGene.of(state, 0, p.states().length))) //
							.toList()));
		}

		return b.build();
	}

	protected static boolean paramsAreValid(Params p) {
		if (p.numberOfPeriods() == 0) {
			// No periods are available
			System.out.println("No periods are available");
			return false;
		}
		if (Arrays.stream(p.productions()).allMatch(v -> v == 0) //
				&& Arrays.stream(p.consumptions()).allMatch(v -> v == 0)) {
			// Production and Consumption predictions are all zero
			System.out.println("Production and Consumption predictions are all zero");
			return false;
		}
		var pricesAreAllTheSame = true;
		for (var i = 1; i < p.prices().length; i++) {
			if (p.prices()[0] != p.prices()[i]) {
				pricesAreAllTheSame = false;
			}
		}
		if (pricesAreAllTheSame) {
			// Prices are all the same
			System.out.println("Prices are all the same");
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

	/**
	 * Calculates the ESS max charge energy for a period.
	 * 
	 * @param essMaxSocEnergy       ESS energy below a configured maximium SoC [Wh]
	 * @param essMaxEnergyPerPeriod ESS max charge/discharge energy per period [Wh]
	 * @param essInitial            ESS initially available energy (SoC in [Wh]) of
	 *                              the given period
	 * @return the value in [Wh]
	 */
	protected static int calculateMaxChargeEnergy(int essMaxSocEnergy, int essMaxEnergyPerPeriod, int essInitial) {
		return IntStream.of(essMaxEnergyPerPeriod, essMaxSocEnergy - essInitial) //
				.map(v -> max(0, v)) // only positives
				.min().orElse(0);
	}

	/**
	 * Calculates the ESS max discharge energy for a period.
	 * 
	 * @param essMinSocEnergy       ESS energy below a configured minimum SoC [Wh]
	 * @param essMaxEnergyPerPeriod ESS max charge/discharge energy per period [Wh]
	 * @param essInitial            ESS initially available energy (SoC in [Wh]) of
	 *                              the given period
	 * @return the value in [Wh]
	 */
	protected static int calculateMaxDischargeEnergy(int essMinSocEnergy, int essMaxEnergyPerPeriod, int essInitial) {
		return IntStream.of(essMaxEnergyPerPeriod, essInitial - essMinSocEnergy) //
				.map(v -> max(0, v)) // only positives
				.min().orElse(0);
	}

	/**
	 * Calculates the ESS charge (negative) or discharge (positive) energy for a
	 * period in {@link StateMachine#BALANCING}.
	 * 
	 * @param essMaxCharge    ESS max charge energy
	 * @param essMaxDischarge ESS max discharge energy
	 * @param production      Production prediction
	 * @param consumption     Consumption prediction
	 * @return the value in [Wh]
	 */
	protected static int calculateBalancingEnergy(int essMaxCharge, int essMaxDischarge, int production,
			int consumption) {
		var balance = consumption - production;
		return fitWithin(-essMaxCharge, essMaxDischarge, balance);
	}

	/**
	 * Calculates the default ESS charge energy per period in
	 * {@link StateMachine#CHARGE_GRID}.
	 * 
	 * <p>
	 * Applies {@link #ESS_CHARGE_C_RATE} with the minimum of usable ESS energy or
	 * predicted consumption energy that cannot be supplied from production.
	 * 
	 * @param essMinSocEnergy ESS energy below a configured minimum SoC [Wh]
	 * @param essMaxSocEnergy ESS energy below a configured maximium SoC [Wh]
	 * @param productions     Production predictions per period
	 * @param consumptions    Consumption predictions per period
	 * @return the value in [Wh]
	 */
	protected static int calculateParamsChargeEnergyInChargeGrid(int essMinSocEnergy, int essMaxSocEnergy,
			int[] productions, int[] consumptions) {
		var usableEssEnergy = max(0, essMaxSocEnergy - essMinSocEnergy);
		var excessConsumptionEnergy = max(0, //
				IntStream.range(0, min(productions.length, consumptions.length)) //
						.map(i -> consumptions[i] - productions[i]) // calculates excess Consumption Energy per Period
						.sum());
		var excessConsumptionEnergyInitial = IntStream.range(0, min(productions.length, consumptions.length)) //
				.takeWhile(i -> consumptions[i] >= productions[i]) // take only first Periods
				.map(i -> consumptions[i] - productions[i]) // calculates excess Consumption Energy per Period
				.sum();

		final int referenceEnergy;
		if (excessConsumptionEnergy > 500) {
			referenceEnergy = excessConsumptionEnergy;
		} else if (excessConsumptionEnergyInitial > 500) {
			referenceEnergy = excessConsumptionEnergyInitial;
		} else {
			referenceEnergy = usableEssEnergy;
		}

		return round(min(usableEssEnergy, referenceEnergy) * ESS_CHARGE_C_RATE / PERIODS_PER_HOUR);
	}

	/**
	 * Calculates the ESS charge energy for one period in
	 * {@link StateMachine#CHARGE_GRID}.
	 * 
	 * @param essMaxCharge     ESS max charge energy [Wh]
	 * @param essChargeInState ESS target charge energy in CHARGE_GRID [Wh]
	 * @param maxBuyFromGrid   Max buy-from-grid energy [Wh]
	 * @param production       Production prediction
	 * @param consumption      Consumption prediction
	 * @return the value in [Wh]
	 */
	protected static int calculateChargeGridEnergy(int essMaxCharge, int essChargeInState, int maxBuyFromGrid,
			int production, int consumption) {
		var remainingAfterChargeProduction = essMaxCharge - max(0, production - consumption);
		var remainingAfterSupplyConsumption = maxBuyFromGrid - max(0, consumption - production);
		var result = IntStream.of(essChargeInState, remainingAfterChargeProduction, remainingAfterSupplyConsumption) //
				.map(v -> max(0, v)) // only positives
				.min().orElse(0);
		return max(1, result); // always at least one, to make a difference to BALANCING
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

		if (optimizer == null) {
			throw new OpenemsException("Has no Schedule");
		}
		final var periods = optimizer.getPeriods();
		if (periods == null) {
			throw new OpenemsException("Has no scheduled Periods");
		}
		final var params = optimizer.getParams();
		if (params == null) {
			throw new OpenemsException("Has no Params");
		}

		// Define channel addresses
		final var channelQuarterlyPrices = new ChannelAddress(componentId, "QuarterlyPrices");
		final var channelStateMachine = new ChannelAddress(componentId, "StateMachine");

		// Query historic data
		final var fromDate = now.minusHours(3);
		var queryResult = timedata.queryHistoricData(null, fromDate, now, //
				Set.of(channelQuarterlyPrices, channelStateMachine, //
						SUM_GRID, SUM_PRODUCTION, SUM_CONSUMPTION, SUM_ESS_DISCHARGE_POWER, SUM_ESS_SOC),
				new Resolution(15, ChronoUnit.MINUTES));
		if (queryResult == null) {
			queryResult = new TreeMap<>();
		}

		// Process past predictions
		var pastPredictions = queryResult.entrySet().stream()//
				.map(Entry::getValue) //
				.map(d -> new ScheduleData(//
						getAsOptionalDouble(d.get(channelQuarterlyPrices)).orElse(null), //
						getAsOptionalInt(d.get(channelStateMachine)).orElse(null), //
						jsonIntToEnergy(d.get(SUM_GRID)), //
						jsonIntToEnergy(d.get(SUM_PRODUCTION)), //
						jsonIntToEnergy(d.get(SUM_CONSUMPTION)), //
						jsonIntToEnergy(d.get(SUM_ESS_DISCHARGE_POWER)), //
						getAsOptionalInt(d.get(SUM_ESS_SOC)).orElse(null))) //
				.toList();
		if (pastPredictions.isEmpty()) {
			IntStream.range(0, 3 /* hours */ * 4 /* quarters */) //
					.mapToObj(i -> new ScheduleData(null, null, null, null, null, null, null)) //
					.toList();
		}

		// Process future predictions
		final var futurePredictions = periods.stream()//
				.map(period -> new ScheduleData(//
						period.price(), //
						period.state().getValue(), //
						period.grid(), //
						period.production(), //
						period.consumption(), //
						period.essChargeDischarge(), //
						round((period.essInitial() * 100) / (float) params.essTotalEnergy()))) // SoC
				.toList();

		// Concatenate past and future predictions
		final var predictions = Stream.concat(//
				pastPredictions.stream().limit(12), // Last 3 hours data.
				futurePredictions.stream()) // Future data.
				.toList();

		// Create schedule and return GetScheduleResponse
		var result = createSchedule(predictions, fromDate);

		return new GetScheduleResponse(requestId, result);
	}

	/**
	 * Generates a 24-hour schedule as a {@link JsonArray}. The resulting schedule
	 * includes timestamped entries of predicted 'state', 'price', 'production',
	 * 'consumption', 'soc' for each 15-minute interval.
	 * 
	 * @param datas     the list of {@link ScheduleData}s
	 * @param timestamp the timestamp of the first entry in the schedule
	 * @return the schedule data as a {@link JsonArray}
	 */
	public static JsonArray createSchedule(List<ScheduleData> datas, ZonedDateTime timestamp) {
		var schedule = JsonUtils.buildJsonArray();

		// Create the JSON object for each ScheduleData and add it to the schedule array
		for (int index = 0; index < datas.size(); index++) {
			var data = datas.get(index);
			schedule.add(JsonUtils.buildJsonObject() //
					// Calculate the timestamp for the current entry, adding 15 minutes for each
					.add("timestamp", toJson(timestamp.plusMinutes(15 * index).format(ISO_INSTANT))) //
					.add("price", toJson(data.quarterlyPrice())) //
					.add("state", toJson(data.stateMachine())) //
					.add("grid", toJson(toPower(data.grid()))) //
					.add("production", toJson(toPower(data.production()))) //
					.add("consumption", toJson(toPower(data.consumption()))) //
					.add("ess", toJson(toPower(data.ess()))) //
					.add("soc", toJson(data.soc())) //
					.build());
		}

		return schedule.build();
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
	 * @param p                        the {@link Params}
	 * @param essChargeDischarge       the scheduled ESS charge/discharge energy for
	 *                                 this period
	 * @param essInitialEnergy         the initial ESS energy in this period
	 * @param balancingChargeDischarge the ESS energy that would be required for
	 *                                 BALANCING
	 * @param state                    the initial state
	 * @return the new state
	 */
	public static StateMachine postprocessSimulatorState(Params p, int essChargeDischarge, int essInitialEnergy,
			int balancingChargeDischarge, StateMachine state) {
		return switch (state) {
		case BALANCING -> state;

		case DELAY_DISCHARGE -> {
			// DELAY_DISCHARGE,...
			if (essInitialEnergy <= p.essMinSocEnergy()) {
				// but battery is already empty (at Min-Soc)
				yield BALANCING;
			} else if (essChargeDischarge < 0 && essChargeDischarge == balancingChargeDischarge) {
				// but actually selling to grid (i.e. production > consumption) -> could have
				// been BALANCING
				yield BALANCING;
			}
			yield state;
		}

		case CHARGE_GRID -> {
			// CHARGE_GRID,...
			if (essChargeDischarge >= 0 || essChargeDischarge == balancingChargeDischarge) {
				// but actually not charging (i.e. no production)
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
			return toPower(params.essChargeInChargeGrid());
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

		return chargePower * -1;
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
	protected static int toEnergy(int power) {
		return power / PERIODS_PER_HOUR;
	}

	private static Integer jsonIntToEnergy(JsonElement j) {
		var i = getAsOptionalInt(j);
		if (i.isPresent()) {
			return toEnergy(i.get());
		}
		return null;
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
	protected static void logSchedule(Params params, TreeMap<ZonedDateTime, Period> periods) {
		var b = new StringBuilder() //
				.append("OPTIMIZER ") //
				.append(params.toString(false)) //
				.append("\n") //
				.append("OPTIMIZER ") //
				.append(Period.header()) //
				.append("\n");
		if (periods.values().isEmpty()) {
			b //
					.append("OPTIMIZER ") //
					.append("-> EMPTY\n");
		} else {
			periods.values().stream() //
					.map(Period::toString) //
					.forEach(s -> b //
							.append("OPTIMIZER ") //
							.append(s) //
							.append("\n"));
		}
		System.out.println(b.toString());
	}
}
