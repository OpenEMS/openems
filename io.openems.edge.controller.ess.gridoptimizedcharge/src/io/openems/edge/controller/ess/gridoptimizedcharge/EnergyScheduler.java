package io.openems.edge.controller.ess.gridoptimizedcharge;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsString;
import static java.util.stream.Collectors.groupingBy;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.OptionalInt;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.ess.gridoptimizedcharge.EnergyScheduler.Config.Automatic;
import io.openems.edge.controller.ess.gridoptimizedcharge.EnergyScheduler.Config.Manual;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

public class EnergyScheduler {

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param parent         the parent {@link OpenemsComponent}
	 * @param configSupplier supplier for {@link Config}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler.WithOnlyOneMode buildEnergyScheduleHandler(OpenemsComponent parent,
			Supplier<Config> configSupplier) {
		return EnergyScheduleHandler.WithOnlyOneMode.<OptimizationContext, Void>create(parent) //
				.setSerializer(() -> Config.toJson(configSupplier.get())) //

				.setOptimizationContext(goc -> {
					// TODO try to reuse existing logic for parsing, calculating limits, etc.; for
					// now this only works for current day and MANUAL mode
					final var config = configSupplier.get();
					final var limits = ImmutableSortedMap.<ZonedDateTime, OptionalInt>naturalOrder();
					final var periodsPerDay = goc.periods().stream() //
							.collect(groupingBy(p -> p.time().truncatedTo(ChronoUnit.DAYS)));
					if (config != null && !periodsPerDay.isEmpty()) {
						final var firstDayMignight = Collections.min(periodsPerDay.keySet());

						for (var entry : periodsPerDay.entrySet()) {
							// Find target time for this day
							var midnight = entry.getKey(); // beginning of this day
							var periods = entry.getValue(); // periods of this day
							ZonedDateTime targetTime = switch (config) {
							case Automatic c -> midnight; // TODO
							case Manual c -> midnight //
									.withHour(c.targetTime.getHour()) //
									.withMinute(c.targetTime.getMinute());
							};
							// Find first period with Production > Consumption
							var firstExcessEnergyOpt = periods.stream() //
									.filter(p -> p.production() > p.consumption()) //
									.findFirst();
							if (firstExcessEnergyOpt.isEmpty()
									|| targetTime.isBefore(firstExcessEnergyOpt.get().time())) {
								// Production exceeds Consumption never or too late on this day
								// -> set no limit for this day
								limits.put(midnight, OptionalInt.empty());
								continue;
							}
							var firstExcessEnergy = firstExcessEnergyOpt.get().time();

							// Set no limit for early hours of the day
							if (firstExcessEnergy.isAfter(midnight)) {
								limits.put(midnight, OptionalInt.empty());
							}

							// Calculate actual charge limit
							var noOfQuarters = (int) Duration.between(firstExcessEnergy, targetTime).toMinutes() / 15;
							if (noOfQuarters == 0) {
								continue;
							}
							final var totalEnergy = midnight == firstDayMignight //
									? // use actual data for first day
									goc.ess().totalEnergy() - goc.ess().currentEnergy()
									: // assume full charge from second day
									goc.ess().totalEnergy();
							limits.put(firstExcessEnergy, OptionalInt.of(totalEnergy / noOfQuarters));

							// No limit after targetTime
							limits.put(targetTime, OptionalInt.empty());
						}
					}
					return new OptimizationContext(limits.build());
				})

				.setSimulator((id, period, gsc, coc, csc, ef, fitness) -> {
					var limitEntry = coc.limits.floorEntry(period.time());
					if (limitEntry == null) {
						return;
					}
					var limit = limitEntry.getValue();
					if (limit.isPresent()) {
						ef.setEssMaxCharge(limit.getAsInt());
					}
				}) //

				.build();
	}

	public sealed interface Config {

		public static record Manual(LocalTime targetTime) implements Config {
		}

		public static record Automatic() implements Config {
		}

		/**
		 * Serialize.
		 * 
		 * @param config the {@link Config}, possibly null
		 * @return the {@link JsonElement}
		 */
		private static JsonElement toJson(Config config) {
			if (config == null) {
				return JsonNull.INSTANCE;
			}
			var b = buildJsonObject() //
					.addProperty("class", config.getClass().getSimpleName());
			switch (config) {
			case Manual moc -> b.addProperty("targetTime", moc.targetTime.toString());
			case Automatic soc -> doNothing();
			}
			return b.build();
		}

		/**
		 * Deserialize.
		 * 
		 * @param j a {@link JsonElement}
		 * @return the {@link Config}
		 */
		public static Config fromJson(JsonElement j) {
			if (j.isJsonNull()) {
				return null;
			}
			try {
				var clazz = getAsString(j, "class");
				if (clazz.equals(Manual.class.getSimpleName())) {
					return new Manual(//
							LocalTime.parse(getAsString(j, "targetTime")));
					// TODO should be a native JsonUtils helper

				} else if (clazz.equals(Automatic.class.getSimpleName())) {
					return new Automatic();

				} else {
					throw new IllegalArgumentException("Unsupported class [" + clazz + "]");
				}
			} catch (OpenemsNamedException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	public static record OptimizationContext(ImmutableSortedMap<ZonedDateTime, OptionalInt> limits) {
		// TODO Should be LocalDateTime to avoid compare issues
	}
}