package io.openems.edge.controller.ortsnetzauslastung;

import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.Instant;

import com.google.gson.JsonObject;

import io.openems.edge.common.meta.types.Coordinates;

public final class Types {

	private Types() {
	}

	/**
	 * Voltage value for phases without measurement, as defined by the
	 * Ortsnetz-Auslastung API.
	 */
	public static final double NO_PHASE_VOLTAGE = -1;

	/**
	 * Measurement data as defined by the Ortsnetz-Auslastung API.
	 * 
	 * <p>
	 * See https://github.com/thomaslehmann1234/datenerfassung-ortsnetz-auslastung/blob/master/API.md
	 */
	public static record SendData(//
			Instant observedAt, //
			Coordinates coordinates, //
			Double l1V, //
			Double l2V, //
			Double l3V, //
			Double gridFrequencyHz, //
			String integrationVersion) {

		/**
		 * Builds the JSON request body for the measurements API.
		 * 
		 * <p>
		 * Missing phase voltages are sent as -1 (for single-phase measurements). The
		 * optional grid frequency is omitted if not available.
		 *
		 * @return the JsonObject
		 */
		public JsonObject toJson() {
			return buildJsonObject() //
					.addProperty("observed_at", this.observedAt.toString()) //
					.addProperty("latitude", this.coordinates.latitude()) //
					.addProperty("longitude", this.coordinates.longitude()) //
					.addProperty("l1_v", asPhaseVoltage(this.l1V)) //
					.addProperty("l2_v", asPhaseVoltage(this.l2V)) //
					.addProperty("l3_v", asPhaseVoltage(this.l3V)) //
					.addPropertyIfNotNull("grid_frequency_hz", this.gridFrequencyHz) //
					.addProperty("integration_version", this.integrationVersion) //
					.build();
		}

		private static double asPhaseVoltage(Double voltage) {
			return voltage == null ? NO_PHASE_VOLTAGE : voltage;
		}
	}
}
