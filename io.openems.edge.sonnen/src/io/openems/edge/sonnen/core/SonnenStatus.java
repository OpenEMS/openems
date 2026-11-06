package io.openems.edge.sonnen.core;

import com.google.gson.JsonElement;

import io.openems.common.utils.JsonUtils;

/**
 * Parsed data of the '/api/v2/status' endpoint of the sonnenBatterie local
 * JSON-API.
 *
 * <p>
 * Sign conventions (per sonnen API documentation):
 *
 * <ul>
 * <li>Pac_total_W: positive = discharging; negative = charging
 * <li>GridFeedIn_W: positive = feed-in; negative = consumption from grid
 * <li>Production_W: positive = PV production
 * </ul>
 */
public record SonnenStatus(//

		// ESS
		Integer pacTotalW, //
		Integer usoc, //
		Integer rsoc, //
		Integer remainingCapacityWh, //
		Boolean batteryCharging, //
		Boolean batteryDischarging, //
		Boolean dischargeNotAllowed, //
		OperatingMode operatingMode, //
		String systemStatus, //

		// PV
		Integer productionW, //

		// Grid
		Integer gridFeedInW, //
		Integer consumptionW, //
		Integer consumptionAvgW, //

		// General
		Float fac, //
		Integer uac, //
		Integer ubat //
) {

	/**
	 * Parses the JSON response of the '/api/v2/status' endpoint.
	 *
	 * @param json the response {@link JsonElement}
	 * @return the parsed {@link SonnenStatus}
	 */
	public static SonnenStatus from(JsonElement json) {
		final var operatingMode = JsonUtils.getAsOptionalInt(json, "OperatingMode") //
				.map(OperatingMode::fromValue) //
				.orElse(OperatingMode.UNDEFINED);
		final var systemStatus = JsonUtils.getAsOptionalString(json, "SystemStatus").orElse(null);

		return new SonnenStatus(//
				JsonUtils.getAsOptionalInt(json, "Pac_total_W").orElse(null), //
				JsonUtils.getAsOptionalInt(json, "USOC").orElse(null), //
				JsonUtils.getAsOptionalInt(json, "RSOC").orElse(null), //
				JsonUtils.getAsOptionalInt(json, "RemainingCapacity_W").orElse(null), //
				JsonUtils.getAsOptionalBoolean(json, "BatteryCharging").orElse(null), //
				JsonUtils.getAsOptionalBoolean(json, "BatteryDischarging").orElse(null), //
				JsonUtils.getAsOptionalBoolean(json, "dischargeNotAllowed").orElse(null), //
				operatingMode, //
				systemStatus, //
				JsonUtils.getAsOptionalInt(json, "Production_W").orElse(null), //
				JsonUtils.getAsOptionalInt(json, "GridFeedIn_W").orElse(null), //
				JsonUtils.getAsOptionalInt(json, "Consumption_W").orElse(null), //
				JsonUtils.getAsOptionalInt(json, "Consumption_Avg").orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "Fac").orElse(null), //
				JsonUtils.getAsOptionalInt(json, "Uac").orElse(null), //
				JsonUtils.getAsOptionalInt(json, "Ubat").orElse(null));
	}

	/**
	 * Checks whether the system is off-grid.
	 *
	 * @return true if off-grid; false if on-grid; null if unknown
	 */
	public Boolean isOffGrid() {
		if (this.systemStatus == null) {
			return null;
		}
		return switch (this.systemStatus) {
		case "OnGrid" -> false;
		case "OffGrid" -> true;
		default -> null;
		};
	}
}
