package io.openems.edge.evcs.hardybarth;

import java.time.LocalDateTime;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

/**
 * Handles writes. Called in every cycle
 */
public class HardyBarthWriteHandler implements Runnable {

	private final HardyBarthImpl parent;
	private Integer lastCurrent = null;
	private LocalDateTime nextCurrentWrite = LocalDateTime.MIN;

	/*
	 * Minimum pause between two consecutive writes.
	 */
	private static final int WRITE_INTERVAL_SECONDS = 1;

	public HardyBarthWriteHandler(HardyBarthImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run() {
		// Return if EVCS communication failed
		if (this.parent.getChargingstationCommunicationFailed().orElse(true)) {
			return;
		}

		this.setManualMode();
		this.setHeartbeat();
		// this.enableExternalMeter();
		this.setEnergyLimit();
		this.setPower();
	}

	/**
	 * Set manual mode.
	 *
	 * <p>
	 * Sets the chargemode to manual if not set.
	 */
	private void setManualMode() {
		StringReadChannel channelChargeMode = this.parent.channel(HardyBarth.ChannelId.RAW_SALIA_CHARGE_MODE);
		var valueOpt = channelChargeMode.value().asOptional();
		if (valueOpt.isPresent()) {
			if (!valueOpt.get().equals("manual")) {
				// Set to manual mode
				try {
					this.parent.debugLog("Setting HardyBarth to manual chargemode");
					JsonElement result = this.parent.api.sendPutRequest("/api/secc", "salia/chargemode", "manual");
					this.parent.debugLog(result.toString());
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
			this.parent.api.sendPutRequest("/api/secc", "salia/heartbeat", "off");
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
	 * Sets the current from SET_CHARGE_POWER channel.
	 *
	 * <p>
	 * Allowed loading current are between 6A and 32A. Invalid values are discarded.
	 * The value is also depending on the configured min and max current of the
	 * charging station.
	 */
	private void setPower() {
		WriteChannel<Integer> energyLimitChannel = this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		int energyLimit = energyLimitChannel.getNextValue().orElse(0);

		// Check energy limit
		if (energyLimit == 0 || energyLimit > this.parent.getEnergySession().orElse(0)) {

			// Check current set_charge_power_limit write value
			WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT);
			var valueOpt = channel.getNextWriteValueAndReset();
			if (valueOpt.isPresent()) {

				int power = valueOpt.get();

				// Convert it to ampere and apply hard limits
				var phases = this.parent.getPhases();
				Integer current = (int) Math.round(power / (double) phases.orElse(3) / 230.0);

				// TODO: Read separate saliaconf.json and set minimum and maximum dynamically
				var maximum = this.parent.config.maxHwCurrent() / 1000;
				var minimum = this.parent.config.minHwCurrent() / 1000;
				if (current > maximum) {
					current = maximum;
				}
				if (current < minimum) {
					current = 0;
				}

				// Send every WRITE_INTERVAL_SECONDS or if the current to send changed
				if (!current.equals(this.lastCurrent) || this.nextCurrentWrite.isBefore(LocalDateTime.now())) {

					this.parent.debugLog("Setting HardyBarth " + this.parent.alias() + " current to [" + current
							+ " A] - calculated from [" + power + " W] by " + phases.orElse(3) + " Phase");

					this.setTarget(current, power);
				}
			}
		} else {
			this.parent.debugLog("Maximum energy limit reached");
			this.parent._setStatus(Status.ENERGY_LIMIT_REACHED);

			if (!this.lastCurrent.equals(0) || this.parent.getChargePower().orElse(0) != 0) {
				this.setTarget(0, 0);
			}
		}
	}

	/**
	 * Set current target to the charger.
	 *
	 * @param current current target in A
	 * @param power   current target in W
	 */
	private void setTarget(int current, int power) {
		try {
			JsonElement resultPause;
			if (current > 0) {
				// Send stop pause request
				resultPause = this.parent.api.sendPutRequest("/api/secc", "salia/pausecharging", "" + 0);
				this.parent.debugLog("Wake up HardyBarth " + this.parent.alias() + " from the pause");
			} else {
				// Send pause charging request
				resultPause = this.parent.api.sendPutRequest("/api/secc", "salia/pausecharging", "" + 1);
				this.parent.debugLog("Setting HardyBarth " + this.parent.alias() + " to pause");
			}

			// Send charge power limit
			JsonElement result = this.parent.api.sendPutRequest("/api/secc", "grid_current_limit", "" + current);

			// Set results
			this.parent._setSetChargePowerLimit(power);
			this.parent.debugLog("Pause: " + resultPause.toString());
			this.parent.debugLog("SetActivePower: " + result.toString());

			// Prepare next write
			this.nextCurrentWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
			this.lastCurrent = current;
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}

	private Integer lastEnergySession = null;

	/**
	 * Sets the nextValue of the SET_ENERGY_LIMIT channel.
	 */
	private void setEnergyLimit() {
		WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);
		var valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			var energyLimit = valueOpt.get();

			// Set if the energy target to set changed
			if (!energyLimit.equals(this.lastEnergySession)) {

				// Set energy limit
				this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT).setNextValue(energyLimit);
				this.parent.debugLog("Setting EVCS " + this.parent.alias() + " Energy Limit in this Session to ["
						+ energyLimit + " Wh]");

				// Prepare next write
				this.lastEnergySession = energyLimit;
			}
		}
	}

}
