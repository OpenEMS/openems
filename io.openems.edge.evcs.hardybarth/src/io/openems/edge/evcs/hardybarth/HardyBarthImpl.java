package io.openems.edge.evcs.hardybarth;

import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.AbstractManagedEvcsComponent;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.HardyBarth", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
})
public class HardyBarthImpl extends AbstractManagedEvcsComponent
		implements OpenemsComponent, EventHandler, HardyBarth, Evcs, ManagedEvcs {

	protected final Logger log = LoggerFactory.getLogger(HardyBarthImpl.class);

	@Reference
	private EvcsPower evcsPower;

	/** API for main REST API functions. */
	protected HardyBarthApi api;
	/** ReadWorker and WriteHandler: Reading and sending data to the EVCS. */
	private final HardyBarthReadWorker readWorker = new HardyBarthReadWorker(this);
	/**
	 * Master EVCS is responsible for RFID authentication (Not implemented for now).
	 */
	protected boolean masterEvcs = true;

	protected Config config;

	public HardyBarthImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				HardyBarth.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setFixedMinimumHardwarePower(config.minHwCurrent() / 1000 * 3 * 230);
		this._setFixedMaximumHardwarePower(config.maxHwCurrent() / 1000 * 3 * 230);
		this._setPowerPrecision(230);

		if (config.enabled()) {
			this.api = new HardyBarthApi(config.ip(), this);

			// Reading the given values
			this.readWorker.activate(config.id());
			this.readWorker.triggerNextRun();
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();

		if (this.readWorker != null) {
			this.readWorker.deactivate();
		}
	}

	@Override
	public void handleEvent(Event event) {

		if (!this.isEnabled()) {
			return;
		}
		super.handleEvent(event);
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:

			this.setManualMode();
			this.setHeartbeat();
			this.readWorker.triggerNextRun();

			// TODO: intelligent firmware update
			break;
		}
	}

	/**
	 * Set manual mode.
	 * 
	 * <p>
	 * Sets the chargemode to manual if not set.
	 */
	private void setManualMode() {
		StringReadChannel channelChargeMode = this.channel(HardyBarth.ChannelId.RAW_SALIA_CHARGE_MODE);
		Optional<String> valueOpt = channelChargeMode.value().asOptional();
		if (valueOpt.isPresent()) {
			if (!valueOpt.get().equals("manual")) {
				// Set to manual mode
				try {
					this.debugLog("Setting HardyBarth to manual chargemode");
					JsonElement result = this.api.sendPutRequest("/api/secc", "salia/chargemode", "manual");
					this.debugLog(result.toString());
				} catch (OpenemsNamedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Set heartbeat.
	 * 
	 * <p>
	 * Sets the heartbeat to on or off.
	 */
	private void setHeartbeat() {
		// The internal heartbeat is currently too fast - it is not enough to write
		// every second by default. We have to disable it to run the evcs
		// properly.
		// TODO: The manufacturer must be asked if it is possible to read the heartbeat
		// status so that we can check if the heartbeat is really disabled and if the
		// heartbeat time can be increased to be able to use this feature.

		try {
			this.api.sendPutRequest("/api/secc", "salia/heartbeat", "off");
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Enable external meter.
	 * 
	 * <p>
	 * Enables the external meter if not set.
	 */
	// TODO: Set the external meter to true because it's disabled per default.
	// Not usable for now, because we haven't an update process defined and
	// this REST Entry is only available with a beta firmware
	// (http://salia.echarge.de/firmware/firmware_1.37.8_beta.image) or the next
	// higher stable version. Be aware that the REST call and the update should not
	// be called every cycle
	/*
	 * private void enableExternalMeter() {
	 * 
	 * BooleanReadChannel channelChargeMode =
	 * this.parent.channel(HardyBarth.ChannelId.RAW_SALIA_CHANGE_METER);
	 * Optional<Boolean> valueOpt = channelChargeMode.value().asOptional(); if
	 * (valueOpt.isPresent()) { if (!valueOpt.get().equals(true)) { // Enable
	 * external meter try {
	 * this.parent.debugLog("Enable external meter of HardyBarth " +
	 * this.parent.id()); JsonElement result =
	 * this.parent.api.sendPutRequest("/api/secc", "salia/changemeter",
	 * "enable | /dev/ttymxc0 | klefr | 9600 | none | 1");
	 * this.parent.debugLog(result.toString());
	 * 
	 * if (result.toString().equals("{\"result\":\"ok\"}")) { // Reboot the charger
	 * this.parent.debugLog("Reboot of HardyBarth " + this.parent.id()); JsonElement
	 * resultReboot = this.parent.api.sendPutRequest("/api/secc",
	 * "salia/servicereboot", "1"); this.parent.debugLog(resultReboot.toString()); }
	 * } catch (OpenemsNamedException e) { e.printStackTrace(); } } } }
	 */

	/**
	 * Debug Log.
	 *
	 * <p>
	 * Logging only if the debug mode is enabled
	 *
	 * @param message text that should be logged
	 */
	public void debugLog(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws OpenemsNamedException {

		// TODO: Use power precision to set valid power if it is used in UI part too
		// e.g. int precision = TypeUtils.getAsType(OpenemsType.INTEGER,
		// this.getPowerPrecision().orElse(230d));
		// power = IntUtils.roundToPrecision(power, Round.TOWARDS_ZERO, precision);

		// Convert it to ampere and apply hard limits
		int phases = this.getPhasesAsInt();
		Integer current = (int) Math.round(power / (double) phases / 230.0);

		return this.setTarget(current);
	}

	@Override
	public boolean pauseChargeProcess() throws OpenemsNamedException {
		return this.setTarget(0);
	}

	/**
	 * Set current target to the charger.
	 * 
	 * @param current current target in A
	 * @return boolean if the target was set
	 * @throws OpenemsNamedException on error
	 */
	private boolean setTarget(int current) throws OpenemsNamedException {

		JsonElement resultPause;
		if (current > 0) {
			// Send stop pause request
			resultPause = this.api.sendPutRequest("/api/secc", "salia/pausecharging", "" + 0);
			this.debugLog("Wake up HardyBarth " + this.alias() + " from the pause");
		} else {
			// Send pause charging request
			resultPause = this.api.sendPutRequest("/api/secc", "salia/pausecharging", "" + 1);
			this.debugLog("Setting HardyBarth " + this.alias() + " to pause");
		}

		// Send charge power limit
		JsonElement resultLimit = this.api.sendPutRequest("/api/secc", "grid_current_limit", "" + current);

		Optional<String> resultLimitVal = JsonUtils.getAsOptionalString(resultLimit, "result");
		Optional<String> resultPauseVal = JsonUtils.getAsOptionalString(resultPause, "result");

		return resultLimitVal.orElse("").equals("ok") && resultPauseVal.orElse("").equals("ok");
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}
}
