package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.common.utils.DateUtils.roundZonedDateTimeDownToMinutes;
import static io.openems.common.utils.JsonUtils.getAsOptionalFloat;
import static io.openems.common.utils.JsonUtils.getAsOptionalInt;
import static io.openems.common.utils.JsonUtils.toJson;
import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static io.openems.edge.common.type.TypeUtils.multiply;
import static io.openems.edge.common.type.TypeUtils.orElse;
import static io.openems.edge.common.type.TypeUtils.subtract;
import static io.openems.edge.common.type.TypeUtils.sum;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.CHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController.PERIODS_PER_HOUR;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.EFFICIENCY_FACTOR;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Arrays.stream;
import static java.util.stream.IntStream.concat;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;

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
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleResponse;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.timedata.api.Timedata;

public final class Utils {

	private Utils() {
	}

	private static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	private static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress("_sum",
			"UnmanagedConsumptionActivePower");
	private static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");

	public record ScheduleData(Float quarterlyPrice, Integer stateMachine, Integer production, Integer consumption,
			Integer soc) {
	}

	/**
	 * Create Params for {@link Simulator}.
	 * 
	 * @param context          the {@link Context} object
	 * @param existingSchedule the existing Schedule, i.e. result of previous
	 *                         optimization
	 * @return {@link Params}
	 * @throws InvalidValueException on error
	 */
	public static Params createSimulatorParams(Context context, TreeMap<ZonedDateTime, Period> existingSchedule)
			throws InvalidValueException {
		final var time = roundZonedDateTimeDownToMinutes(ZonedDateTime.now(), 15);

		// Prediction values
		final var predictionProduction = context.predictorManager() //
				.get24HoursPrediction(SUM_PRODUCTION).getValues();
		final var predictionConsumption = joinConsumptionPredictions(4, //
				context.predictorManager().get24HoursPrediction(SUM_CONSUMPTION).getValues(), //
				context.predictorManager().get24HoursPrediction(SUM_UNMANAGED_CONSUMPTION).getValues());

		// Prices contains the price values and the time it is retrieved.
		final var prices = context.timeOfUseTariff().getPrices();

		// Ess information.
		final var netEssCapacity = context.ess().getCapacity().getOrError();
		final var soc = context.ess().getSoc().getOrError();

		// Calculate available energy using "netCapacity" and "soc".
		var currentAvailableEnergy = netEssCapacity /* [Wh] */ / 100 * soc;

		final var reduceAbove = 10; // TODO make this configurable via Risk Level

		// Calculate the net usable energy of the battery.
		final var reduceAboveEnergy = netEssCapacity / 100 * reduceAbove;
		final var limitEnergy = getLimitEnergy(context, netEssCapacity) + reduceAboveEnergy;
		final var netUsableEnergy = max(0, netEssCapacity - limitEnergy);

		// Calculate current usable energy [Wh] in the battery.
		currentAvailableEnergy = max(0, currentAvailableEnergy - limitEnergy);

		// Power Values for scheduling battery for individual periods.
		// TODO store max ever charge/dischare power
		var power = context.ess().getPower();
		var dischargePower = max(5000 /* at least 5000 */, power.getMaxPower(context.ess(), Phase.ALL, Pwr.ACTIVE));
		var chargePower = min(-5000 /* at least 5000 */, power.getMinPower(context.ess(), Phase.ALL, Pwr.ACTIVE));

		var maxChargePowerFromGrid = context.maxChargePowerFromGrid();

		return Params.create() //
				.time(time) //
				.essAvailableEnergy(currentAvailableEnergy) //
				.essCapacity(netUsableEnergy) //
				.essMaxEnergyPerPeriod(toEnergy(max(dischargePower, abs(chargePower)))) //
				.maxBuyFromGrid(toEnergy(maxChargePowerFromGrid)) //
				.productions(stream(interpolateArray(predictionProduction)).map(v -> toEnergy(v)).toArray()) //
				.consumptions(stream(interpolateArray(predictionConsumption)).map(v -> toEnergy(v)).toArray()) //
				.prices(interpolateArray(prices.getValues())) //
				.states(context.controlMode().states) //
				.existingSchedule(existingSchedule) //
				.build();
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
	 * Returns the amount of energy that is not usable for scheduling.
	 * 
	 * @param context        the {@link Context}
	 * @param netEssCapacity net capacity of the battery.
	 * @return the amount of energy that is limited.
	 */
	private static int getLimitEnergy(Context context, int netEssCapacity) {
		// Usable capacity based on minimum SoC from Limit total discharge and emergency
		// reserve controllers.
		var limitSoc = concat(//
				context.ctrlLimitTotalDischarges().stream().mapToInt(ctrl -> ctrl.getMinSoc().orElse(0)), //
				context.ctrlEmergencyCapacityReserves().stream().mapToInt(ctrl -> ctrl.getActualReserveSoc().orElse(0))) //
				.max().orElse(0);

		return netEssCapacity /* [Wh] */ / 100 * limitSoc;
	}

	/**
	 * Interpolate an Array of {@link Float}s.
	 * 
	 * <p>
	 * Replaces nulls with previous value. If first entry is null, it is set to
	 * first available value. If all values are null, all are set to 0.
	 * 
	 * @param values the values
	 * @return values without nulls
	 */
	protected static float[] interpolateArray(Float[] values) {
		var firstNonNull = stream(values) //
				.filter(Objects::nonNull) //
				.findFirst();
		var lastNonNullIndex = IntStream.range(0, values.length) //
				.filter(i -> values[i] != null) //
				.reduce((first, second) -> second); //
		if (lastNonNullIndex.isEmpty()) {
			return new float[0];
		}
		var result = new float[lastNonNullIndex.getAsInt() + 1];
		if (firstNonNull.isEmpty()) {
			// all null
			return result;
		}
		float last = firstNonNull.get();
		for (var i = 0; i < result.length; i++) {
			float value = orElse(values[i], last);
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
	 * Calculates the ESS charge energy for one period in
	 * {@link StateMachine#CHARGE} state.
	 * 
	 * @param maxBuyFromGrid Max Buy-From-Grid Energy per Period [Wh]
	 * @param consumption    Consumption prediction
	 * @param production     Production prediction
	 * @param essMaxCharge   the max ESS charge energy after constraints; always
	 *                       negative
	 * @return ESS charge energy
	 */
	protected static int calculateStateChargeEnergy(int maxBuyFromGrid, int consumption, int production,
			int essMaxCharge) {
		return max(//
				min(//
						/* max charge energy from grid */
						-maxBuyFromGrid + consumption - production,
						/* force positive */
						1),
				/* limit to max ESS charge energy */
				essMaxCharge);
	}

	/**
	 * Calculates the ActivePower constraint for CHARGE state.
	 * 
	 * @param ess                    the {@link ManagedSymmetricEss}
	 * @param sum                    the {@link Sum}
	 * @param maxChargePowerFromGrid the configured max charge from grid power
	 * @return the set-point or null
	 */
	public static Integer calculateCharge100(ManagedSymmetricEss ess, Sum sum, int maxChargePowerFromGrid) {
		// Calculate 'real' grid-power (without current ESS charge/discharge)
		var gridPower = sum(//
				sum.getGridActivePower().get(), /* current buy-from/sell-to grid */
				ess.getActivePower().get() /* current charge/discharge Ess */);

		return min(0, // never positive, i.e. force discharge
				subtract(gridPower, maxChargePowerFromGrid)); // apply maxChargePowerFromGrid
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
	 * @param fromDate    the From-Date
	 * @param now         the To-Date
	 * @return the {@link GetScheduleResponse}
	 * @throws OpenemsNamedException on error
	 */
	public static GetScheduleResponse handleGetScheduleRequest(Optimizer optimizer, UUID requestId, Timedata timedata,
			String componentId, ZonedDateTime fromDate, ZonedDateTime now) throws OpenemsNamedException {
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

		final var essUsableCapacity = params.essCapacity();

		// Define channel addresses
		final var channelQuarterlyPrices = new ChannelAddress(componentId, "QuarterlyPrices");
		final var channelStateMachine = new ChannelAddress(componentId, "StateMachine");
		final var channelPredictedProduction = new ChannelAddress(componentId, "PredictedProduction");
		final var channelPredictedConsumption = new ChannelAddress(componentId, "PredictedConsumption");

		// Collect channels in a set
		final var channels = Set.of(channelQuarterlyPrices, channelStateMachine, channelPredictedProduction,
				channelPredictedConsumption);

		// Query historic data
		final var queryResult = timedata.queryHistoricData(null, fromDate, now, channels,
				new Resolution(15, ChronoUnit.MINUTES));

		// Process past predictions
		final var pastPredictions = queryResult.entrySet().stream()//
				.map(Entry::getValue) //
				.map(d -> new ScheduleData(//
						getAsOptionalFloat(d.get(channelQuarterlyPrices)).orElse(null), //
						getAsOptionalInt(d.get(channelStateMachine)).orElse(null), //
						getAsOptionalInt(d.get(channelPredictedProduction)).orElse(null), //
						getAsOptionalInt(d.get(channelPredictedConsumption)).orElse(null), //
						null)) //
				.collect(Collectors.toList());

		// Process future predictions
		final var futurePredictions = periods.stream()//
				.map(period -> new ScheduleData(//
						period.price(), //
						period.state().getValue(), //
						period.production(), //
						period.consumption(), //
						(period.essInitial() * 100) / essUsableCapacity)) //
				.collect(Collectors.toList());

		// Concatenate past and future predictions
		final var predictions = Stream.concat(//
				pastPredictions.stream(), // Last 3 hours data.
				futurePredictions.stream()) // Future data.
				.limit(96) // Limits the total data to 24 hours.
				.collect(Collectors.toList());

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
					.add("production", toJson(toPower(data.production()))) //
					.add("consumption", toJson(toPower(data.consumption()))) //
					.add("soc", toJson(data.soc())) //
					.build());
		}

		return schedule.build();
	}

	/**
	 * Post-Process a state of a Period, i.e. replace with 'better' state with the
	 * same behaviour.
	 * 
	 * <p>
	 * NOTE: heavy computation is ok here, because this method is called only at the
	 * end with the best Schedule.
	 * 
	 * @param p                        the {@link Params}
	 * @param essInitial               the initial ESS energy in this period
	 * @param gridEssCharge            grid-buy energy that is used to charge the
	 *                                 ESS
	 * @param price                    the price in this period
	 * @param balancingChargeDischarge the ESS energy that would be required for
	 *                                 BALANCING
	 * @param state                    the initial state
	 * @return the new state
	 */
	public static StateMachine postprocessPeriodState(Params p, int essInitial, int gridEssCharge, float price,
			int balancingChargeDischarge, StateMachine state) {
		var soc = essInitial * 100 / p.essCapacity();
		return switch (state) {
		case BALANCING -> BALANCING;

		case DELAY_DISCHARGE -> {
			// DELAY_DISCHARGE,...
			if (balancingChargeDischarge < 0) {
				// but actually charging from PV -> could have been BALANCING
				yield BALANCING;
			} else if (p.maxPrice() - price < 0.001F) {
				// but price is high
				yield BALANCING;
			}
			yield DELAY_DISCHARGE;
		}

		case CHARGE -> {
			// CHARGE,...
			if (gridEssCharge <= 0) {
				// but actually charging from PV -> could have been BALANCING
				yield BALANCING;
			} else if (soc >= 90) {
				// but battery was already > 90 % SoC -> too risky
				yield DELAY_DISCHARGE;
			} else if (p.maxPrice() / price < EFFICIENCY_FACTOR) {
				// but price is high
				yield DELAY_DISCHARGE;
			}
			yield CHARGE;
		}
		};
	}

	/**
	 * Gets the current existingSchedule (i.e. the bestGenotype of last optimization
	 * run) as {@link Genotype} to serve as initial population.
	 * 
	 * @param p the {@link Params}
	 * @return the {@link Genotype}
	 */
	public static List<Genotype<IntegerGene>> buildInitialPopulation(Params p) {
		var states = List.of(p.states());
		return List.of(//
				Genotype.of(//
						IntStream.range(0, p.numberOfPeriods()) //
								// Map to state index; not-found maps to '-1', corrected to '0'
								.map(i -> fitWithin(0, p.states().length, states.indexOf(//
										p.existingSchedule().length > i ? p.existingSchedule()[i] //
												: 0 // fill remaining with '0'
								))) //
								.mapToObj(state -> IntegerChromosome.of(IntegerGene.of(state, 0, p.states().length))) //
								.toList()));
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

	/**
	 * Converts energy [Wh/15 min] to power [W].
	 * 
	 * @param energy the energy value
	 * @return the power value
	 */
	public static Integer toPower(Integer energy) {
		return multiply(energy, PERIODS_PER_HOUR);
	}
}
