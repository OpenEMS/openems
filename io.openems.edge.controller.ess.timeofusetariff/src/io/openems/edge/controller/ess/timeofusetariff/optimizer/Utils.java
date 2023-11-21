package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.common.utils.DateUtils.roundZonedDateTimeDownToMinutes;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.common.type.TypeUtils.orElse;
import static io.openems.edge.common.type.TypeUtils.subtract;
import static io.openems.edge.common.type.TypeUtils.sum;
import static io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController.PERIODS_PER_HOUR;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.stream;
import static java.util.stream.IntStream.concat;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.SortedMap;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Streams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.timeofusetariff.ScheduleUtils;
import io.openems.edge.controller.ess.timeofusetariff.jsonrpc.GetScheduleResponse;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public final class Utils {

	private Utils() {
	}

	private static final ChannelAddress SUM_PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	private static final ChannelAddress SUM_UNMANAGED_CONSUMPTION = new ChannelAddress("_sum",
			"UnmanagedConsumptionActivePower");
	private static final ChannelAddress SUM_CONSUMPTION = new ChannelAddress("_sum", "ConsumptionActivePower");

	/**
	 * Create Params for {@link Simulator}.
	 * 
	 * @param context the {@link Context} object
	 * @return Params
	 * @throws InvalidValueException on error
	 */
	public static Params createSimulatorParams(Context context) throws InvalidValueException {
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

	protected static int toEnergy(int power) {
		return power / PERIODS_PER_HOUR;
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
	 * @param optimizer           the {@link Optimizer}
	 * @param requestId           the JSON-RPC request-id
	 * @param queryResult         the historic data.
	 * @param channelPrices       the {@link ChannelAddress} for Quarterly prices.
	 * @param channelStateMachine the {@link ChannelAddress} for the state machine.
	 * @return the {@link GetScheduleResponse}
	 * @throws OpenemsException on error
	 */
	public static GetScheduleResponse handleGetScheduleRequest(Optimizer optimizer, UUID requestId,
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult, ChannelAddress channelPrices,
			ChannelAddress channelStateMachine) throws OpenemsException {
		if (optimizer == null) {
			throw new OpenemsException("Has no Schedule");
		}
		var periods = optimizer.getPeriods();
		if (periods == null) {
			throw new OpenemsException("Has no scheduled Periods");
		}

		// Extract the price data
		var priceValuesPast = queryResult.values().stream() //
				// Only specific channel address values.
				.map(t -> t.get(channelPrices)) //
				// get as Array
				.collect(toJsonArray());

		// Extract the State Machine data
		var stateMachineValuesPast = queryResult.values().stream() //
				// Only specific channel address values.
				.map(t -> t.get(channelStateMachine)) //
				// Mapping to absolute state machine values since query result gives average
				// values.
				.map(t -> {
					if (t.isJsonPrimitive() && t.getAsJsonPrimitive().isNumber()) {
						// 'double' to 'int' for appropriate state machine values.
						return new JsonPrimitive(t.getAsInt());
					}

					return JsonNull.INSTANCE;
				})
				// get as Array
				.collect(toJsonArray());

		final var stateMachineValues = new JsonArray();
		final var priceValues = new JsonArray();

		// Create StateMachine for future values based on schedule created.
		periods.forEach(period -> {
			priceValues.add(period.price());
			stateMachineValues.add(period.state().getValue());
		});

		var prices = Stream.concat(//
				JsonUtils.stream(priceValuesPast), // Last 3 hours data.
				JsonUtils.stream(priceValues)) // Next 21 hours data.
				.limit(96) //
				.collect(toJsonArray());

		var states = Stream.concat(//
				JsonUtils.stream(stateMachineValuesPast), // Last 3 hours data
				JsonUtils.stream(stateMachineValues)) // Next 21 hours data.
				.limit(96) //
				.collect(toJsonArray());

		var timestamp = queryResult.firstKey();
		var result = ScheduleUtils.createSchedule(prices, states, timestamp);

		return new GetScheduleResponse(requestId, result);
	}

}
