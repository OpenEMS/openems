package io.openems.edge.io.shelly.common.component;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsOptionalFloat;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static java.lang.Math.round;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.utils.StringUtils;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.shelly.common.HttpBridgeShellyService;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

public class ShellyMeteredSwitchHandler {
	private final Logger log = LoggerFactory.getLogger(ShellyMeteredSwitchHandler.class);

	private final ShellyMeteredSwitch component;
	private final HttpBridgeShellyService shellyService;
	private final int index;
	private final boolean shouldInvert;

	private final BooleanWriteChannel[] digitalOutputChannels;
	private final CalculateEnergyFromPower calculateProductionEnergy;
	private final CalculateEnergyFromPower calculateConsumptionEnergy;

	public ShellyMeteredSwitchHandler(ShellyMeteredSwitch component, HttpBridgeShellyService shellyService, int index,
			boolean shouldInvert) {
		this.component = component;
		this.shellyService = shellyService;
		this.index = index;
		this.shouldInvert = shouldInvert;

		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.component.channel(ShellySwitch.ChannelId.RELAY) //
		};
		this.calculateProductionEnergy = new CalculateEnergyFromPower(this.component,
				ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
		this.calculateConsumptionEnergy = new CalculateEnergyFromPower(this.component,
				ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this.component);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this.component);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this.component);
	}

	/**
	 * Handles event from eventlistener.
	 * 
	 * @param event event to handle
	 */
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		case TOPIC_CYCLE_EXECUTE_WRITE -> this.executeWrite();
		}
	}

	/**
	 * Reads switch data from shelly json switch data.
	 * 
	 * @param json Switch data from shelly json
	 * @throws OpenemsError.OpenemsNamedException thrown if json is invalid
	 */
	public void processSwitchData(JsonObject json) throws OpenemsError.OpenemsNamedException {
		final var shelly = this.component;

		var relayStatus = getAsBoolean(json, "output");
		setValue(shelly, ShellySwitch.ChannelId.RELAY, relayStatus);

		// NOTE: Consumption data can be missing in switch object. It happens
		// sometimes when relais was never activated since shelly restart.
		Integer fallbackPowerValue = relayStatus ? null : 0;

		setValue(shelly, ElectricityMeter.ChannelId.ACTIVE_POWER, getAsOptionalFloat(json, "apower") //
				.map(v -> this.invert(round(v))) //
				.orElse(fallbackPowerValue));
		setValue(shelly, ElectricityMeter.ChannelId.CURRENT, getAsOptionalFloat(json, "current") //
				.map(v -> this.invert(round(v * 1000F))) //
				.orElse(fallbackPowerValue));
		setValue(shelly, ElectricityMeter.ChannelId.VOLTAGE, getAsOptionalFloat(json, "voltage") //
				.map(v -> round(v * 1000F)) //
				.orElse(null));

		this.processErrors(json);
	}

	private void processErrors(JsonObject json) throws OpenemsError.OpenemsNamedException {
		var errorKeys = new HashSet<String>();
		if (json.has("errors")) {
			var jsonErrors = getAsJsonArray(json, "errors");
			for (var jsonError : jsonErrors) {
				errorKeys.add(jsonError.getAsString());
			}
		}

		for (var errorChannel : ShellyMeteredSwitch.ErrorChannelId.values()) {
			if (!StringUtils.isNullOrEmpty(errorChannel.getShellyErrorCode())) {
				setValue(this.component, errorChannel, errorKeys.remove(errorChannel.getShellyErrorCode()));
			}
		}

		if (!errorKeys.isEmpty()) {
			this.log.warn("[%s] Received unknown shelly error codes: %s".formatted(this.component.id(),
					String.join(", ", errorKeys)));
		}
	}

	/**
	 * Resets all data values in case of failure.
	 */
	public void resetSwitchData() {
		final var shelly = this.component;
		setValue(shelly, ShellySwitch.ChannelId.RELAY, null);
		setValue(shelly, ElectricityMeter.ChannelId.ACTIVE_POWER, null);
		setValue(shelly, ElectricityMeter.ChannelId.CURRENT, null);
		setValue(shelly, ElectricityMeter.ChannelId.VOLTAGE, null);
	}

	/**
	 * Generates a standard Debug-Log string for Shellys with one relay and power
	 * meter.
	 *
	 * @return suitable for {@link OpenemsComponent#debugLog()}
	 */
	public String generateDebugLog() {
		var b = new StringBuilder();
		for (int i = 0; i < this.digitalOutputChannels.length; i++) {
			var relayChannel = this.digitalOutputChannels[i];
			relayChannel.value().asOptional().ifPresentOrElse(v -> b.append(v ? "x" : "-"), () -> b.append("?"));
			if (i < this.digitalOutputChannels.length - 1) {
				b.append("|");
			}
		}
		b.append("|").append(this.component.getActivePowerChannel().value().asString());
		return b.toString();
	}

	public BooleanWriteChannel[] getDigitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	private int invert(int value) {
		return this.shouldInvert ? -value : value;
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
	 * Executes a write command to a specified relay channel by constructing and
	 * sending an HTTP request based on the channel's current and intended state.
	 * This method compares the current state with the desired state, and only
	 * proceeds with the HTTP request if they differ, ensuring no unnecessary
	 * commands are sent. The method returns a CompletableFuture that completes when
	 * the HTTP request is finished. It completes normally if the HTTP request
	 * succeeds, and exceptionally if the request fails due to errors.
	 *
	 * @return CompletableFuture{@code <Void>} that completes when the HTTP
	 *         operation completes. Completes exceptionally if there is an error in
	 *         the HTTP request.
	 */
	private CompletableFuture<Void> executeWrite() {
		if (this.shellyService == null) {
			return CompletableFuture.completedFuture(null);
		}
		Boolean readValue = this.component.getRelayChannel().value().get();
		Optional<Boolean> writeValue = this.component.getRelayChannel().getNextWriteValueAndReset();

		if (writeValue.isEmpty()) {
			// No action needed
			return CompletableFuture.completedFuture(null);
		}
		if (Objects.equals(readValue, writeValue.get())) {
			// No change in state
			return CompletableFuture.completedFuture(null);
		}

		return this.shellyService.setSwitchStatus(this.index, writeValue.get()).thenAccept(FunctionUtils::doNothing);
	}
}
