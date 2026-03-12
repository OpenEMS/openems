package io.openems.edge.io.shelly.shellyplugsbase;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalFloat;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonObject;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.io.shelly.common.Utils.executeWrite;
import static io.openems.edge.io.shelly.common.Utils.generateDebugLog;
import static java.lang.Math.round;

import java.util.function.IntFunction;

import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBaseImpl;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.types.DebugMode;
import io.openems.common.types.MeterType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

/**
 * Base class for shelly plugs gen2 and gen3. Implements meter values and relay.
 */
public abstract class IoShellyPlugSBaseImpl extends IoGen2ShellyBaseImpl implements IoShellyPlugSBase, DigitalOutput,
		SinglePhaseMeter, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(IoShellyPlugSBaseImpl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private MeterType meterType = null;
	private SinglePhase phase = null;
	private boolean invert = false;

	protected IoShellyPlugSBaseImpl(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds //
	) {
		super(//
				firstInitialChannelIds, //
				furtherInitialChannelIds //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShellyPlugSBase.ChannelId.RELAY) //
		};

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, MeterType type,
			SinglePhase phase, boolean invert, String ip, String mdnsName, DebugMode debugMode,
			boolean enableDeviceValidation) {
		this.meterType = type;
		this.phase = phase;
		this.invert = invert;

		super.activate(context, id, alias, enabled, ip, mdnsName, debugMode, enableDeviceValidation);
	}

	@Override
	protected void subscribeDataCalls() {
		this.cycleService.subscribeJsonEveryCycle(this.baseUrl + "/rpc/Shelly.GetStatus", this::processHttpResult);
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this.digitalOutputChannels, this.getActivePowerChannel()) //
				+ (this.metricService != null ? ", " + this.metricService : "");
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		case TOPIC_CYCLE_EXECUTE_WRITE -> executeWrite(this.getRelayChannel(), this.baseUrl, this.httpBridge, 0);
		}
	}

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		if (this.getWrongDeviceType().orElse(false)) {
			// do not apply values if device-type is wrong
			this.resetValues();
			return;
		}

		if (error != null) {
			this.logWarn(this.log, error.getMessage());
			this.resetValues();
			return;
		}

		final IntFunction<Integer> invert = value -> this.invert ? value * -1 : value;

		Boolean relayStatus = null;
		boolean updatesAvailable = false;
		Integer activePower = null;
		Integer current = null;
		Integer voltage = null;
		try {
			var response = getAsJsonObject(result.data());
			var sysInfo = getAsJsonObject(response, "sys");
			var update = getAsJsonObject(sysInfo, "available_updates");
			var stable = getAsOptionalJsonObject(update, "stable");
			updatesAvailable = (stable.isPresent() && !stable.isEmpty());

			var relays = getAsJsonObject(response, "switch:0");
			relayStatus = getAsBoolean(relays, "output");

			// NOTE: Consumption data can be missing in switch object. It happens
			// sometimes when relais was never activated since shelly restart.

			Integer fallbackPowerValue = relayStatus ? null : 0;
			activePower = getAsOptionalFloat(relays, "apower").map(v -> invert.apply(round(v)))
					.orElse(fallbackPowerValue);
			current = getAsOptionalFloat(relays, "current").map(v -> invert.apply(round(v * 1000)))
					.orElse(fallbackPowerValue);
			voltage = getAsOptionalFloat(relays, "voltage").map(v -> round(v * 1000)).orElse(null);

		} catch (Exception e) {
			this.logWarn(this.log, e.getMessage());
		}

		this.updateValues(relayStatus, activePower, current, voltage, updatesAvailable);
	}

	private void resetValues() {
		this.updateValues(null, null, null, null, false);
	}

	private void updateValues(Boolean relayStatus, Integer activePower, Integer current, Integer voltage,
			boolean updatesAvailable) {
		this._setRelay(relayStatus);
		this._setActivePower(activePower);
		this._setCurrent(current);
		this._setVoltage(voltage);
		this.channel(IoShellyPlugSBase.ChannelId.HAS_UPDATE).setNextValue(updatesAvailable);
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		final var activePower = this.getActivePower().get();
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

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public SinglePhase getPhase() {
		return this.phase;
	}
}