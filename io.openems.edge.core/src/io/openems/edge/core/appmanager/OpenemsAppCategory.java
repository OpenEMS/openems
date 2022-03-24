package io.openems.edge.core.appmanager;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public enum OpenemsAppCategory {

	// TODO translation

	/**
	 * Integrated Systems.
	 */
	INTEGRATED_SYSTEM("Integrierte Systeme"),

	/**
	 * Time variable energy price.
	 */
	TIME_VARIABLE_PRICE("Zeitvariable Stromtarife"),

	/**
	 * Electric vehicle charging station.
	 */
	EVCS("E-Mobilität"),

	/**
	 * Load control.
	 */
	HEAT("Wärme"),

	/**
	 * Hardware.
	 */
	HARDWARE("Hardware"),

	/**
	 * PV-Inverter.
	 */
	PV_INVERTER("PV-Wechselrichter"),

	/**
	 * Meter.
	 */
	METER("Zähler");

	private String readableName;

	private OpenemsAppCategory(String readableName) {
		this.readableName = readableName;
	}

	public String getReadableName() {
		return this.readableName;
	}

	/**
	 * Creates a {@link JsonObject} of the {@link OpenemsAppCategory}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("name", this.name()) //
				.addProperty("readableName", this.getReadableName()) //
				.build();
	}

}
