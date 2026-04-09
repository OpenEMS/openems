package io.openems.edge.io.shelly.common.component;

import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonArray;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static java.lang.Math.round;

import java.util.HashSet;
import java.util.Set;

import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

public class ShellyEnergyMeterHandler {
	private final Logger log = LoggerFactory.getLogger(ShellyEnergyMeterHandler.class);

	private final ShellyEnergyMeter component;
	private final boolean shouldInvert;

	private final CalculateEnergyFromPower calculateProductionEnergy;
	private final CalculateEnergyFromPower calculateConsumptionEnergy;

	public ShellyEnergyMeterHandler(ShellyEnergyMeter component, boolean shouldInvert) {
		this.component = component;
		this.shouldInvert = shouldInvert;

		this.calculateProductionEnergy = new CalculateEnergyFromPower(this.component,
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
		this.calculateConsumptionEnergy = new CalculateEnergyFromPower(this.component,
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

		ElectricityMeter.calculateSumCurrentFromPhases(this.component);
		ElectricityMeter.calculateAverageVoltageFromPhases(this.component);
	}

	/**
	 * Handles event from eventlistener.
	 *
	 * @param event event to handle
	 */
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		}
	}

	/**
	 * Reads energy meter data from shelly EM.GetStatus response.
	 *
	 * @param json EM.GetStatus response
	 * @throws OpenemsError.OpenemsNamedException thrown if json is invalid
	 */
	public void processEmData(JsonObject json) throws OpenemsError.OpenemsNamedException {
		final var shelly = this.component;
		final var errors = new HashSet<ShellyEnergyMeter.ErrorChannelId>();

		// Check for 'errors' and process if present
		getAsOptionalJsonArray(json, "errors") //
				.ifPresent(x -> this.parseShellyErrors(x, errors));

		// Total Active Power
		setValue(shelly, ElectricityMeter.ChannelId.ACTIVE_POWER,
				this.invert(round(getAsFloat(json, "total_act_power"))));
		setValue(shelly, ShellyEnergyMeter.ChannelId.APPARENT_POWER, round(getAsFloat(json, "total_aprt_power")));

		// Extract phase data
		setValue(shelly, ElectricityMeter.ChannelId.ACTIVE_POWER_L1,
				this.invert(round(getAsFloat(json, "a_act_power"))));
		setValue(shelly, ShellyEnergyMeter.ChannelId.APPARENT_POWER_L1, round(getAsFloat(json, "a_aprt_power")));
		setValue(shelly, ElectricityMeter.ChannelId.VOLTAGE_L1, round(getAsFloat(json, "a_voltage") * 1000));
		setValue(shelly, ElectricityMeter.ChannelId.CURRENT_L1,
				this.invert(round(getAsFloat(json, "a_current") * 1000)));
		getAsOptionalJsonArray(json, "a_errors") //
				.ifPresent(x -> this.parseShellyErrors(x, "a_errors(%s)", errors));

		setValue(shelly, ElectricityMeter.ChannelId.ACTIVE_POWER_L2,
				this.invert(round(getAsFloat(json, "b_act_power"))));
		setValue(shelly, ShellyEnergyMeter.ChannelId.APPARENT_POWER_L2, round(getAsFloat(json, "b_aprt_power")));
		setValue(shelly, ElectricityMeter.ChannelId.VOLTAGE_L2, round(getAsFloat(json, "b_voltage") * 1000));
		setValue(shelly, ElectricityMeter.ChannelId.CURRENT_L2,
				this.invert(round(getAsFloat(json, "b_current") * 1000)));
		getAsOptionalJsonArray(json, "b_errors") //
				.ifPresent(x -> this.parseShellyErrors(x, "b_errors(%s)", errors));

		setValue(shelly, ElectricityMeter.ChannelId.ACTIVE_POWER_L3,
				this.invert(round(getAsFloat(json, "c_act_power"))));
		setValue(shelly, ShellyEnergyMeter.ChannelId.APPARENT_POWER_L3, round(getAsFloat(json, "c_aprt_power")));
		setValue(shelly, ElectricityMeter.ChannelId.VOLTAGE_L3, round(getAsFloat(json, "c_voltage") * 1000));
		setValue(shelly, ElectricityMeter.ChannelId.CURRENT_L3,
				this.invert(round(getAsFloat(json, "c_current") * 1000)));
		getAsOptionalJsonArray(json, "c_errors") //
				.ifPresent(x -> this.parseShellyErrors(x, "c_errors(%s)", errors));

		this.setShellyErrors(errors);
	}

	/**
	 * Resets all em data values in case of failure.
	 */
	public void resetEmData() {
		final var shelly = this.component;

		setValue(shelly, ElectricityMeter.ChannelId.ACTIVE_POWER, null);
		setValue(shelly, ShellyEnergyMeter.ChannelId.APPARENT_POWER, null);

		setValue(shelly, ElectricityMeter.ChannelId.ACTIVE_POWER_L1, null);
		setValue(shelly, ShellyEnergyMeter.ChannelId.APPARENT_POWER_L1, null);
		setValue(shelly, ElectricityMeter.ChannelId.VOLTAGE_L1, null);
		setValue(shelly, ElectricityMeter.ChannelId.CURRENT_L1, null);

		setValue(shelly, ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null);
		setValue(shelly, ShellyEnergyMeter.ChannelId.APPARENT_POWER_L2, null);
		setValue(shelly, ElectricityMeter.ChannelId.VOLTAGE_L2, null);
		setValue(shelly, ElectricityMeter.ChannelId.CURRENT_L2, null);

		setValue(shelly, ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null);
		setValue(shelly, ShellyEnergyMeter.ChannelId.APPARENT_POWER_L3, null);
		setValue(shelly, ElectricityMeter.ChannelId.VOLTAGE_L3, null);
		setValue(shelly, ElectricityMeter.ChannelId.CURRENT_L3, null);
	}

	private int invert(int value) {
		return this.shouldInvert ? -value : value;
	}

	private void parseShellyErrors(JsonArray jsonErrors, Set<ShellyEnergyMeter.ErrorChannelId> resultList) {
		this.parseShellyErrors(jsonErrors, "%s", resultList);
	}

	private void parseShellyErrors(JsonArray jsonErrors, String errorKeyFormat,
			Set<ShellyEnergyMeter.ErrorChannelId> resultList) {
		for (var jsonError : jsonErrors) {
			try {
				var shellyErrorCode = errorKeyFormat.formatted(getAsString(jsonError));
				var errorChannel = ShellyEnergyMeter.ErrorChannelId.byShellyErrorCode(shellyErrorCode);
				if (errorChannel.isEmpty()) {
					this.log.warn("Can't find shelly error code '{}'", shellyErrorCode);
					continue;
				}

				resultList.add(errorChannel.get());
			} catch (Exception ex) {
				this.log.warn("Error while parsing shelly error code (" + jsonError + "): " + ex.getMessage());
			}
		}
	}

	private void setShellyErrors(Set<ShellyEnergyMeter.ErrorChannelId> channelsWithError) {
		for (var errorChannel : ShellyEnergyMeter.ErrorChannelId.valuesWithShellyErrorCode()) {
			setValue(this.component, errorChannel, channelsWithError.contains(errorChannel));
		}
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		final var activePower = this.component.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(-activePower);
		}
	}

	/**
	 * Generates a standard Debug-Log string for Shellys with energy meter.
	 *
	 * @return suitable for {@link OpenemsComponent#debugLog()}
	 */
	public String generateDebugLog() {
		return "L:" + this.component.getActivePower().asString();
	}
}
