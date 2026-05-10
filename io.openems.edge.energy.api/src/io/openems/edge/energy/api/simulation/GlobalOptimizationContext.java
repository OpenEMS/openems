package io.openems.edge.energy.api.simulation;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.energy.api.simulation.periods.PeriodDuration.HOUR;
import static io.openems.edge.energy.api.simulation.periods.PeriodDuration.QUARTER;

import java.time.Clock;
import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.meta.GridBuySoftLimit;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.LogVerbosity;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.periods.PeriodData;
import io.openems.edge.energy.api.simulation.periods.PeriodDuration;
import io.openems.edge.energy.api.simulation.periods.Periods;

/**
 * Holds the context that is used globally for an entire optimization run.
 * 
 * <p>
 * This record is usually created once per quarter.
 */
public record GlobalOptimizationContext(//
		Clock clock, //
		Environment environment,
		/** Start-Timestamp */
		ZonedDateTime startTime, //
		ImmutableList<EnergyScheduleHandler> eshs, //
		ImmutableList<EnergyScheduleHandler.WithDifferentModes> eshsWithDifferentModes, //
		Grid grid, //
		Ess ess, //
		Periods periods) {

	/**
	 * Serialize.
	 * 
	 * @return the {@link JsonObject}
	 */
	public static JsonElement toJson(GlobalOptimizationContext goc) {
		return buildJsonObject() //
				.addProperty("zone", goc.clock.getZone().getId()) //
				.addProperty("environment", goc.environment) //
				.addProperty("startTime", goc.startTime) //
				.add("grid", goc.grid, Grid.serializer(goc.clock)) //
				.add("ess", goc.ess, Ess.serializer()) //
				.add("eshs", goc.eshs.stream() //
						.map(EnergyScheduleHandler::toJson) //
						.collect(toJsonArray())) //
				.build();
	}

	public record Grid(//
			/** Max Buy-From-Grid Power [W] */
			int maxBuyPower, //
			/** Max Sell-To-Grid Power [W] */
			int maxSellPower,
			/** The Grid-Buy Soft-Limit [W] */
			JSCalendar.Tasks<GridBuySoftLimit> gridBuySoftLimit) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Grid}.
		 * 
		 * @param clock the {@link Clock}
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Grid> serializer(Clock clock) {
			return jsonObjectSerializer(Grid.class, json -> {
				return new Grid(//
						json.getInt("maxBuyPower"), //
						json.getInt("maxSellPower"), //
						json.getObject("gridBuySoftLimit", GridBuySoftLimit.tasksSerializer(clock)));
			}, obj -> {
				return buildJsonObject() //
						.addProperty("maxBuyPower", obj.maxBuyPower) //
						.addProperty("maxSellPower", obj.maxSellPower) //
						.add("gridBuySoftLimit", obj.gridBuySoftLimit, GridBuySoftLimit.tasksSerializer(clock)) //
						.build();
			});
		}
	}

	public record Ess(//
			/** ESS Currently Available Energy (SoC in [Wh]) */
			int currentEnergy, //
			/** ESS Total Energy (Capacity) [Wh] */
			int totalEnergy, //
			/** ESS Max Charge Power [W] */
			int maxChargePower, //
			/** ESS Max Discharge Power [W] */
			int maxDischargePower) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Ess}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Ess> serializer() {
			return jsonObjectSerializer(Ess.class, json -> {
				return new Ess(//
						json.getInt("currentEnergy"), //
						json.getInt("totalEnergy"), //
						json.getInt("maxChargePower"), //
						json.getInt("maxDischargePower"));
			}, obj -> {
				return buildJsonObject() //
						.addProperty("currentEnergy", obj.currentEnergy) //
						.addProperty("totalEnergy", obj.totalEnergy) //
						.addProperty("maxChargePower", obj.maxChargePower) //
						.addProperty("maxDischargePower", obj.maxDischargePower) //
						.build();
			});
		}
	}

	/**
	 * Returns a builder for {@link GlobalOptimizationContext}.
	 * 
	 * @return a {@link GocBuilder}
	 */
	public static GocBuilder builder() {
		return new GocBuilder(LogVerbosity.NONE);
	}

	/**
	 * Returns a builder for {@link GlobalOptimizationContext}.
	 * 
	 * @param logVerbosity the {@link LogVerbosity}
	 * @return a {@link GocBuilder}
	 */
	public static GocBuilder builder(LogVerbosity logVerbosity) {
		return new GocBuilder(logVerbosity);
	}

	public sealed interface Period {

		/**
		 * Returns the duration type of this period (e.g. {@link PeriodDuration#QUARTER}
		 * or {@link PeriodDuration#HOUR}).
		 *
		 * @return the period duration
		 */
		PeriodDuration duration();

		/**
		 * Returns the index of this period.
		 *
		 * @return the period index
		 */
		int index();

		/**
		 * Returns the start timestamp of this period.
		 *
		 * @return the start time as {@link ZonedDateTime}
		 */
		ZonedDateTime time();

		/**
		 * Returns the grid-buy soft limit of this period.
		 *
		 * @return the soft limit in Wh, or {@code null}
		 */
		Integer gridBuySoftLimit();

		/**
		 * Returns the data of this period.
		 *
		 * @return the period data
		 */
		PeriodData data();

		record Quarter(//
				int index, //
				ZonedDateTime time, //
				Integer gridBuySoftLimit, //
				PeriodData data) implements Period {

			@Override
			public PeriodDuration duration() {
				return QUARTER;
			}
		}

		record Hour(//
				int index, //
				ZonedDateTime time, //
				Integer gridBuySoftLimit, //
				PeriodData data, //
				ImmutableList<Quarter> quarterPeriods) implements Period {

			@Override
			public PeriodDuration duration() {
				return HOUR;
			}
		}
	}
}
