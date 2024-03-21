package io.openems.edge.controller.ess.timeofusetariff.jsonrpc;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.types.OptionsEnum.getOption;
import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.toJson;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_ESS_DISCHARGE_POWER;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_ESS_SOC;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_GRID;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_PRODUCTION;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.toEnergy;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Math.round;
import static java.util.Optional.ofNullable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Optimizer;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.Period;

/**
 * Data for JSONRPC-Response. Values are in [W].
 */
public record ScheduleDatas(int essTotalEnergy, ImmutableList<ScheduleData> entries) {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	/**
	 * Creates {@link ScheduleDatas} from an {@link Optimizer}.
	 * 
	 * @param optimizer the {@link Optimizer}
	 * @return a {@link ScheduleDatas}a
	 * @throws OpenemsException on error
	 */
	public static ScheduleDatas fromSchedule(Optimizer optimizer) throws OpenemsException {
		final var schedule = optimizer.getSchedule();
		if (schedule == null) {
			throw new OpenemsException("Has no Schedule");
		}
		final var params = optimizer.getParams();
		if (params == null) {
			throw new OpenemsException("Has no Params");
		}
		return fromSchedule(params.essTotalEnergy(), schedule);
	}

	/**
	 * Creates {@link ScheduleDatas} from a Schedule of {@link Period}s.
	 * 
	 * @param essTotalEnergy ESS Total Energy (Capacity) [Wh]
	 * @param schedule       the {@link Period}s
	 * @return a list of {@link ScheduleData}
	 */
	public static ScheduleDatas fromSchedule(int essTotalEnergy, ImmutableSortedMap<ZonedDateTime, Period> schedule) {
		return new ScheduleDatas(//
				essTotalEnergy, //
				schedule.values().stream() //
						.flatMap(ScheduleData::fromPeriod) //
						.collect(toImmutableList()));
	}

	/**
	 * Creates {@link ScheduleDatas} from the log output of {@link #toLogString()}.
	 * 
	 * @param essTotalEnergy ESS Total Energy (Capacity) [Wh]
	 * @param log            the log output of {@link #toLogString()}
	 * @return a list of {@link ScheduleData}
	 */
	public static ScheduleDatas fromLogString(int essTotalEnergy, String log) throws IllegalArgumentException {
		return new ScheduleDatas(//
				essTotalEnergy, //
				log.lines() //
						.filter(l -> !l.contains("Params") && !l.contains("Time")) //
						.map(l -> {
							var matcher = PATTERN.matcher(l);
							if (!matcher.find()) {
								throw new IllegalArgumentException("Pattern does not match");
							}
							return new ScheduleData(//
									LocalTime.parse(matcher.group("time"), FORMATTER) //
											.atDate(LocalDate.MIN).atZone(ZoneId.of("UTC")), //
									null /* ignore */, //
									parseInt(matcher.group("essMaxEnergy")), //
									parseInt(matcher.group("maxBuyFromGrid")), //
									parseInt(matcher.group("essInitial")), //
									parseInt(matcher.group("production")), //
									parseInt(matcher.group("consumption")), //
									parseDouble(matcher.group("price")), //
									StateMachine.valueOf(matcher.group("state")), //
									parseInt(matcher.group("essChargeDischarge")), //
									parseInt(matcher.group("grid"))); //
						}) //
						.collect(toImmutableList()));
	}

	private static final Pattern PATTERN = Pattern.compile("" //
			+ "(?<time>\\d{2}:\\d{2})" //
			+ "\\s+(?<optimizeBy>-?[\\w-]+)" //
			+ "\\s+(?<essMaxEnergy>-?\\d+)" //
			+ "\\s+(?<maxBuyFromGrid>-?\\d+)" //
			+ "\\s+(?<essInitial>-?\\d+)" //
			+ "\\s+(?<production>-?\\d+)" //
			+ "\\s+(?<consumption>-?\\d+)" //
			+ "\\s+(?<price>-?\\d+\\.\\d+)" //
			+ "\\s+(?<state>-?\\w+)" //
			+ "\\s+(?<essChargeDischarge>-?\\d+)" //
			+ "\\s+(?<grid>-?\\d+)");

	/**
	 * Builds a log string of this {@link ScheduleDatas}.
	 * 
	 * @return log string
	 */
	public String toLogString(String prefix) {
		var b = new StringBuilder(prefix) //
				.append("Time  OptimizeBy EssMaxEnergy MaxBuyFromGrid EssInitial Production Consumption  Price State           EssChargeDischarge  Grid\n");
		this.entries.forEach(e -> b //
				.append(prefix) //
				.append(String.format(Locale.ENGLISH, "%s %-10s %12d %14d %10d %10d %11d %6.2f %-17s %16d %5d\n", //
						e.time().format(FORMATTER), //
						e.length() == null ? "-" : e.length().name(), //
						e.essMaxEnergy(), //
						e.maxBuyFromGrid(), //
						e.essInitial(), //
						e.production(), //
						e.consumption(), //
						e.price(), //
						e.state().name(), //
						e.essChargeDischarge(), //
						e.grid())));
		return b.toString();
	}

	/**
	 * Builds a {@link JsonArray} of this {@link ScheduleDatas}.
	 * 
	 * @param fromDate the From-Date
	 * @return {@link JsonArray}
	 */
	public JsonArray toJsonArray(ZonedDateTime fromDate) {
		var result = JsonUtils.buildJsonArray();
		var nextTime = fromDate;

		// Create the JSON object for each ScheduleData and add it to the schedule array
		for (var e : this.entries) {
			while (nextTime.isBefore(e.time())) {
				// Add empty object for gaps
				result.add(JsonUtils.buildJsonObject() //
						.addProperty("timestamp", nextTime) //
						.add("soc", JsonNull.INSTANCE) //
						.add("production", JsonNull.INSTANCE) //
						.add("consumption", JsonNull.INSTANCE) //
						.add("state", JsonNull.INSTANCE) //
						.add("price", JsonNull.INSTANCE) //
						.add("ess", JsonNull.INSTANCE) //
						.add("grid", JsonNull.INSTANCE) //
						.build());
				nextTime = nextTime.plusMinutes(15);
			}

			result.add(JsonUtils.buildJsonObject() //
					.addProperty("timestamp", e.time()) //
					.add("soc", toJson(round((e.essInitial() * 100) / (float) this.essTotalEnergy()))) //
					.add("production", toJson(e.production())) //
					.add("consumption", toJson(e.consumption())) //
					.add("state", toJson(e.state.getValue())) //
					.add("price", toJson(e.price())) //
					.add("ess", toJson(e.essChargeDischarge())) //
					.add("grid", toJson(e.grid())) //
					.build());
			nextTime = e.time().plusMinutes(15);
		}

		return result.build();
	}

	private static enum Length {
		HOUR, QUARTER
	}

	public record ScheduleData(//
			/** Timestamp of the record */
			ZonedDateTime time,
			/** Record was optimized by QUARTER/HOUR */
			Length length,
			/** ESS Max Charge/Discharge Energy [Wh] */
			int essMaxEnergy, //
			/** Max Buy-From-Grid Energy [Wh] */
			int maxBuyFromGrid,
			/** ESS Initially Available Energy (SoC in [Wh]) */
			int essInitial,
			/** Production prediction [Wh] */
			int production,
			/** Consumption prediction [Wh] */
			int consumption,
			/** Price [1/MWh] */
			double price,
			/** State of the record */
			StateMachine state,
			/** ESS Charge/Discharge Energy [Wh] */
			int essChargeDischarge, //
			/** Resulting grid Energy [Wh] */
			int grid //
	) {

		/**
		 * Creates a Stream of {@link ScheduleData} from a historic data query.
		 * 
		 * @param essTotalEnergy ESS Total Energy (Capacity) [Wh]
		 * @param time           the {@link ZonedDateTime} of the Period
		 * @param period         the {@link Period}
		 * @return a Stream of {@link ScheduleData}
		 */
		public static Stream<ScheduleData> fromHistoricDataQuery(ZonedDateTime fromDate, int essTotalEnergy,
				ChannelAddress channelQuarterlyPrices, ChannelAddress channelStateMachine,
				SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult)
				throws IllegalArgumentException {
			if (queryResult == null) {
				queryResult = new TreeMap<>();
			}

			return queryResult.entrySet().stream() //
					.map(e -> {
						var d = e.getValue();
						Function<ChannelAddress, JsonElement> getter = (c) -> ofNullable(d.get(c))
								.orElse(JsonNull.INSTANCE);

						try {
							return new ScheduleData(//
									e.getKey(), //
									null /* ignore */, //
									0 /* ignore */, //
									0 /* ignore */, //
									round(getAsInt(getter.apply(SUM_ESS_SOC)) / 100F * essTotalEnergy), //
									toEnergy(getAsInt(getter.apply(SUM_PRODUCTION))), //
									toEnergy(getAsInt(getter.apply(SUM_CONSUMPTION))), //
									getAsDouble(getter.apply(channelQuarterlyPrices)), //
									ofNullable(getOption(StateMachine.class, //
											getAsInt(getter.apply(channelStateMachine))))
											.orElse(StateMachine.BALANCING),
									toEnergy(getAsInt(getter.apply(SUM_ESS_DISCHARGE_POWER))), //
									toEnergy(getAsInt(getter.apply(SUM_GRID))) //
							);
						} catch (OpenemsNamedException e1) {
							return null;
						}
					}) //
					.filter(Objects::nonNull);
		}

		/**
		 * Creates a Stream of {@link ScheduleData}.
		 * 
		 * @param essTotalEnergy ESS Total Energy (Capacity) [Wh]
		 * @param period         the {@link Period}
		 * @return a Stream of {@link ScheduleData}
		 */
		public static Stream<ScheduleData> fromPeriod(Period period) {
			var op = period.op();
			var qps = op.quarterPeriods();
			return qps.stream() //
					.map(qp -> {
						var factor = 1F / qps.size();
						return new ScheduleData(//
								qp.time(), //
								qps.size() == 1 ? Length.QUARTER : Length.HOUR, //
								qp.essMaxEnergy(), //
								qp.maxBuyFromGrid(), //
								period.essInitial(), //
								qp.production(), //
								qp.consumption(), //
								qp.price(), //
								period.state(), //
								round(factor * period.ef().ess()), //
								round(factor * period.ef().grid()));
					});
		}
	}

	/**
	 * See {@link Collection#isEmpty()}.
	 * 
	 * @return isEmpty
	 */
	public boolean isEmpty() {
		return this.entries().isEmpty();
	}

	/**
	 * See {@link Collection#stream()}.
	 * 
	 * @return stream
	 */
	public Stream<ScheduleData> stream() {
		return this.entries().stream();
	}
}