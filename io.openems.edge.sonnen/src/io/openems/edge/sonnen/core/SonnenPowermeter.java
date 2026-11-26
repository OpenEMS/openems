package io.openems.edge.sonnen.core;

import com.google.gson.JsonElement;

import io.openems.common.utils.JsonUtils;

/**
 * Parsed entry of the '/api/v2/powermeter' endpoint of the sonnenBatterie local
 * JSON-API.
 *
 * <p>
 * Sign convention: 'w' values are positive in the direction of the meter; i.e.
 * positive = import (consumption) for the 'consumption' meter and positive =
 * production for the 'production' meter. 'kwh_imported' and 'kwh_exported'
 * hold the cumulative imported/exported energy in [kWh].
 */
public record SonnenPowermeter(//

		String direction, //
		Integer wTotal, //
		Integer wL1, //
		Integer wL2, //
		Integer wL3, //
		Integer varTotal, //
		Float currentL1, //
		Float currentL2, //
		Float currentL3, //
		Float voltageL1N, //
		Float voltageL2N, //
		Float voltageL3N, //
		Long kwhImported, //
		Long kwhExported //
) {

	/**
	 * Parses one entry of the JSON response of the '/api/v2/powermeter'
	 * endpoint.
	 *
	 * @param json one entry of the response array
	 * @return the parsed {@link SonnenPowermeter}
	 */
	public static SonnenPowermeter from(JsonElement json) {
		return new SonnenPowermeter(//
				JsonUtils.getAsOptionalString(json, "direction").orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "w_total") //
						.map(f -> (int) Math.round(f)).orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "w_l1") //
						.map(f -> (int) Math.round(f)).orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "w_l2") //
						.map(f -> (int) Math.round(f)).orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "w_l3") //
						.map(f -> (int) Math.round(f)).orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "var_total") //
						.map(f -> (int) Math.round(f)).orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "a_l1").orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "a_l2").orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "a_l3").orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "v_l1_n").orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "v_l2_n").orElse(null), //
				JsonUtils.getAsOptionalFloat(json, "v_l3_n").orElse(null), //
				JsonUtils.getAsOptionalLong(json, "kwh_imported").orElse(null), //
				JsonUtils.getAsOptionalLong(json, "kwh_exported").orElse(null));
	}

	/**
	 * Checks whether this entry is the 'consumption' meter (grid).
	 *
	 * @return true for 'consumption'; false for 'production'
	 */
	public boolean isConsumption() {
		return !"production".equals(this.direction);
	}
}
