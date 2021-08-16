package io.openems.edge.evcs.hardybarth;

import java.time.LocalDateTime;
import java.util.Optional;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
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
	private static final int WRITE_INTERVAL_SECONDS = 30;

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
		Optional<String> valueOpt = channelChargeMode.value().asOptional();
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
			Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();
			if (valueOpt.isPresent()) {

				int power = valueOpt.get();

				// Convert it to ampere and apply hard limits
				Value<Integer> phases = this.parent.getPhases();
				Integer current = (int) Math.round(power / (double) phases.orElse(3) / 230.0);

				// TODO: Read separate saliaconf.json and set minimum and maximum dynamically
				int maximum = this.parent.config.maxHwCurrent() / 1000;
				int minimum = this.parent.config.minHwCurrent() / 1000;
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
			// Send charge power limit
			JsonElement result = this.parent.api.sendPutRequest("/api/secc", "grid_current_limit", "" + current);

			// Set results
			this.parent._setSetChargePowerLimit(current);
			this.parent.debugLog(result.toString());

			// Prepare next write
			this.nextCurrentWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
			this.lastCurrent = current;
			this.parent._setSetChargePowerLimit(power);
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
		Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			Integer energyLimit = valueOpt.get();

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
