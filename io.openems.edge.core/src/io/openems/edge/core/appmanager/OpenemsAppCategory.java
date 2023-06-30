package io.openems.edge.core.appmanager;

import java.util.ResourceBundle;

import com.google.gson.JsonObject;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;

public enum OpenemsAppCategory {

	/**
	 * Integrated Systems.
	 */
	INTEGRATED_SYSTEM("integratedSystems"),

	/**
	 * Time variable energy price.
	 */
	TIME_OF_USE_TARIFF("timeOfUseTariff"),

	/**
	 * Electric vehicle charging station.
	 */
	EVCS("evcs"),

	/**
	 * Heat.
	 */
	HEAT("heat"),

	/**
	 * Ess Controller.
	 */
	ESS("ess"),

	/**
	 * Load Control.
	 */
	LOAD_CONTROL("loadControl"),

	/**
	 * Hardware.
	 */
	HARDWARE("hardware"),

	/**
	 * PV-Inverter.
	 */
	PV_INVERTER("pvInverter"),

	/**
	 * PV self-consumption.
	 */
	PV_SELF_CONSUMPTION("pvSelfConsumption"),

	/**
	 * Meter.
	 */
	METER("meter"),

	/**
	 * Apis.
	 */
	API("api"),

	/**
	 * Category for test apps.
	 *
	 * <p>
	 * NOTE: Do not use this category for normal apps!
	 */
	TEST("test");

	private String readableNameKey;

	private OpenemsAppCategory(String readableNameKey) {
		this.readableNameKey = readableNameKey;
	}

	/**
	 * Gets the readable name in the specific language.
	 *
	 * @param language the language of the name
	 * @return the name
	 */
	public String getReadableName(Language language) {
		var translationBundle = ResourceBundle.getBundle("io.openems.edge.core.appmanager.translation",
				language.getLocal());
		return TranslationUtil.getTranslation(translationBundle, this.readableNameKey);
	}

	/**
	 * Creates a {@link JsonObject} of the {@link OpenemsAppCategory}.
	 *
	 * @param language the language of the readable name
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject(Language language) {
		return JsonUtils.buildJsonObject() //
				.addProperty("name", this.name()) //
				.addProperty("readableName", this.getReadableName(language)) //
				.build();
	}

}
