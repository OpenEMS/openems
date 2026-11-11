package io.openems.edge.controller.ortsnetzauslastung;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.Instant;

import com.google.gson.JsonObject;

import io.openems.common.types.OptionsEnum;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;
import io.openems.edge.common.meta.types.Coordinates;

public final class Types {

	private Types() {
	}

	/**
	 * Value for phases without measurement, as defined by the Ortsnetz-Auslastung
	 * API.
	 */
	public static final double NO_PHASE_VALUE = -1;

	/**
	 * Minimum, maximum and arithmetic mean of a value over the send interval.
	 *
	 * @param min the minimum
	 * @param max the maximum
	 * @param avg the arithmetic mean; min &lt;= avg &lt;= max
	 */
	public static record Statistics(double min, double max, double avg) {
	}

	/**
	 * Static values of the location. They are sent on start and whenever they
	 * change, as defined by the Ortsnetz-Auslastung API.
	 *
	 * @param batteryCapacityKwh usable battery capacity in [kWh] or null
	 * @param smartmeterModel    model of the grid meter or null
	 */
	public static record StaticData(//
			Double batteryCapacityKwh, //
			String smartmeterModel) {

		private void addTo(JsonObjectBuilder builder) {
			builder //
					.addPropertyIfNotNull("battery_capacity", this.batteryCapacityKwh) //
					.addPropertyIfNotNull("smartmeter_model", this.smartmeterModel);
		}
	}

	/**
	 * Measurement data as defined by the Ortsnetz-Auslastung API.
	 *
	 * <p>
	 * All values are already validated; null means "not available".
	 *
	 * <p>
	 * See
	 * https://github.com/thomaslehmann1234/datenerfassung-ortsnetz-auslastung/blob/master/API.md
	 *
	 * @param observedAt         the timestamp
	 * @param coordinates        the location
	 * @param l1V                L1 voltage in [V] or null
	 * @param l2V                L2 voltage in [V] or null
	 * @param l3V                L3 voltage in [V] or null
	 * @param l1VStatistics      L1 voltage statistics or null
	 * @param l2VStatistics      L2 voltage statistics or null
	 * @param l3VStatistics      L3 voltage statistics or null
	 * @param gridFrequencyHz    grid frequency in [Hz] or null
	 * @param frequencyStatistic grid frequency statistics or null
	 * @param gridPowerW         active power at the grid connection point in [W]
	 *                           (positive = buy-from-grid) or null
	 * @param l1A                L1 current in [A] or null
	 * @param l2A                L2 current in [A] or null
	 * @param l3A                L3 current in [A] or null
	 * @param batterySocPercent  battery state of charge in [%] or null
	 * @param staticData         static data or null if it should not be sent
	 * @param integrationVersion the integration version
	 */
	public static record SendData(//
			Instant observedAt, //
			Coordinates coordinates, //
			Double l1V, //
			Double l2V, //
			Double l3V, //
			Statistics l1VStatistics, //
			Statistics l2VStatistics, //
			Statistics l3VStatistics, //
			Double gridFrequencyHz, //
			Statistics frequencyStatistic, //
			Integer gridPowerW, //
			Double l1A, //
			Double l2A, //
			Double l3A, //
			Integer batterySocPercent, //
			StaticData staticData, //
			String integrationVersion) {

		/**
		 * Builds the JSON request body for the measurements API.
		 *
		 * <p>
		 * Missing phase voltages are sent as -1 (for single-phase measurements); other
		 * missing values are omitted.
		 *
		 * @return the JsonObject
		 */
		public JsonObject toJson() {
			final var builder = buildJsonObject() //
					.addProperty("observed_at", this.observedAt.toString()) //
					.addProperty("latitude", this.coordinates.latitude()) //
					.addProperty("longitude", this.coordinates.longitude()) //
					.addProperty("l1_v", orNoPhaseValue(this.l1V)) //
					.addProperty("l2_v", orNoPhaseValue(this.l2V)) //
					.addProperty("l3_v", orNoPhaseValue(this.l3V));
			addStatistics(builder, "l1_v", this.l1VStatistics);
			addStatistics(builder, "l2_v", this.l2VStatistics);
			addStatistics(builder, "l3_v", this.l3VStatistics);
			builder.addPropertyIfNotNull("grid_frequency_hz", this.gridFrequencyHz);
			addStatistics(builder, "grid_frequency_hz", this.frequencyStatistic);
			builder.addPropertyIfNotNull("grid_power_w", this.gridPowerW);
			if (this.l1A != null || this.l2A != null || this.l3A != null) {
				builder //
						.addProperty("l1_a", orNoPhaseValue(this.l1A)) //
						.addProperty("l2_a", orNoPhaseValue(this.l2A)) //
						.addProperty("l3_a", orNoPhaseValue(this.l3A));
			}
			builder.addPropertyIfNotNull("battery_soc_percent", this.batterySocPercent);
			if (this.staticData != null) {
				this.staticData.addTo(builder);
			}
			return builder //
					.addProperty("integration_version", this.integrationVersion) //
					.build();
		}

		private static double orNoPhaseValue(Double value) {
			return value == null ? NO_PHASE_VALUE : value;
		}

		private static void addStatistics(JsonObjectBuilder builder, String prefix, Statistics statistics) {
			if (statistics == null) {
				return;
			}
			builder //
					.addProperty(prefix + "_min", statistics.min()) //
					.addProperty(prefix + "_max", statistics.max()) //
					.addProperty(prefix + "_avg", statistics.avg());
		}
	}

	/**
	 * Traffic light status of the grid voltage ("status" in the API response).
	 */
	public static enum GridStatus implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		GREEN(0, "green"), //
		YELLOW(1, "yellow"), //
		RED(2, "red");

		private final int value;
		private final String name;

		private GridStatus(int value, String name) {
			this.value = value;
			this.name = name;
		}

		/**
		 * Parses the API value.
		 *
		 * @param value the value, e.g. "green"; may be null
		 * @return the {@link GridStatus}; UNDEFINED if unknown
		 */
		public static GridStatus fromApi(String value) {
			for (var status : values()) {
				if (status != UNDEFINED && status.name.equalsIgnoreCase(value)) {
					return status;
				}
			}
			return UNDEFINED;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}

	/**
	 * Storage recommendation ("storage_recommendation" in the API response).
	 */
	public static enum StorageRecommendation implements OptionsEnum {
		UNDEFINED(-1, "Undefined"), //
		NONE(0, "none"), //
		CHARGE(1, "charge"), //
		DISCHARGE(2, "discharge");

		private final int value;
		private final String name;

		private StorageRecommendation(int value, String name) {
			this.value = value;
			this.name = name;
		}

		/**
		 * Parses the API value.
		 *
		 * @param value the value, e.g. "charge"; may be null
		 * @return the {@link StorageRecommendation}; UNDEFINED if unknown
		 */
		public static StorageRecommendation fromApi(String value) {
			for (var recommendation : values()) {
				if (recommendation != UNDEFINED && recommendation.name.equalsIgnoreCase(value)) {
					return recommendation;
				}
			}
			return UNDEFINED;
		}

		@Override
		public int getValue() {
			return this.value;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public OptionsEnum getUndefined() {
			return UNDEFINED;
		}
	}
}
