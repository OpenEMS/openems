package io.openems.edge.evcs.keba.kecontact;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

/**
 * Handles writes. Called in every cycle
 */
public class WriteHandler implements Runnable {

	private final Logger log = LoggerFactory.getLogger(WriteHandler.class);

	private final KebaKeContact parent;

	/*
	 * Minimum pause between two consecutive writes.
	 */
	private static final int WRITE_INTERVAL_SECONDS = 5;
	private static final int WRITE_DISPLAY_INTERVAL_SECONDS = 60;

	public WriteHandler(KebaKeContact parent) {
		this.parent = parent;
	}

	@Override
	public void run() {

		Channel<Boolean> communicationChannel = this.parent
				.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED);
		if (communicationChannel.value().orElse(true)) {
			return;
		}
		this.setDisplay();
		this.setPower();
		this.setEnergySession();
	}

	private String lastDisplay = null;
	private LocalDateTime nextDisplayWrite = LocalDateTime.MIN;

	/**
	 * Sets the display text from SET_DISPLAY channel.
	 *
	 * <p>
	 * Note:
	 * <ul>
	 * <li>Maximum 23 ASCII characters can be used.
	 * <li>If you use the text 'kWh', it will be replaced with '???' (due to MID
	 * metering certification)
	 * </ul>
	 */
	private void setDisplay() {
		WriteChannel<String> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_DISPLAY_TEXT);
		var valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			var display = valueOpt.get();
			if (display.length() > 23) {
				display = display.substring(0, 23);
			}
			display = display.replace(" ", "$"); // $ == blank
			if (!display.equals(this.lastDisplay) || this.nextDisplayWrite.isBefore(LocalDateTime.now())) {

				this.parent.logInfoInDebugmode(this.log, "Setting KEBA KeContact display text to [" + display + "]");

				var sentSuccessfully = this.parent.send("display 0 0 0 0 " + display);
				if (sentSuccessfully) {
					this.nextDisplayWrite = LocalDateTime.now().plusSeconds(WRITE_DISPLAY_INTERVAL_SECONDS);
					this.lastDisplay = display;
				}
			}
		}
	}

	private Integer lastCurrent = null;
	private LocalDateTime nextCurrentWrite = LocalDateTime.MIN;

	/**
	 * Sets the current from SET_CHARGE_POWER channel.
	 *
	 * <p>
	 * Allowed loading current are between 6000mA and 63000mA. Invalid values are
	 * discarded. The value is also depending on the DIP-switch settings and the
	 * used cable of the charging station.
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

				var power = valueOpt.get();
				var phases = this.parent.getPhases();
				Integer current = power * 1000 / phases.orElse(3) /* e.g. 3 phases */ / 230; /* voltage */
				// limits the charging value because KEBA knows only values between 6000 and
				// 63000
				if (current > 63000) {
					current = 63000;
				}
				if (current < 6000) {
					current = 0;
				}

				if (!current.equals(this.lastCurrent) || this.nextCurrentWrite.isBefore(LocalDateTime.now())) {

					this.parent.logInfoInDebugmode(this.log, "Setting KEBA " + this.parent.alias() + " current to ["
							+ current + " A] - calculated from [" + power + " W] by " + phases.orElse(3) + " Phase");

					this.setTarget(current, power);
				}
			}
		} else {
			try {
				this.parent.setDisplayText(energyLimit + " Wh limit reached");
			} catch (OpenemsNamedException e) {
				e.printStackTrace();
			}
			this.parent.logInfoInDebugmode(this.log, "Maximum energy limit reached");
			this.parent._setStatus(Status.ENERGY_LIMIT_REACHED);

			if (!this.lastCurrent.equals(0) || this.parent.getChargePower().orElse(0) != 0) {
				this.setTarget(0, 0);
			}
		}
	}

	/**
	 * Set current target to the charger.
	 *
	 * @param current current target in mA
	 * @param power   current target in W
	 */
	private void setTarget(int current, int power) {
		try {
			Channel<Integer> currPower = this.parent.channel(KebaChannelId.ACTUAL_POWER);
			this.parent.setDisplayText(currPower.value().orElse(0) / 1000 + "W");
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}

		var sentSuccessfully = this.parent.send("currtime " + current + " 1");
		if (sentSuccessfully) {
			this.nextCurrentWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
			this.lastCurrent = current;
			this.parent._setSetChargePowerLimit(power);
		}
	}

	private Integer lastEnergySession = null;

	/**
	 * Sets the Energy Limit for this session from SET_ENERGY_SESSION channel.
	 */
	private void setEnergySession() {

		WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT);

		var valueOpt = channel.getNextWriteValueAndReset();

		if (valueOpt.isPresent()) {
			var energyLimit = valueOpt.get();

			// Set if the energy target to set changed
			if (!energyLimit.equals(this.lastEnergySession)) {

				// Set energy limit
				this.parent.channel(ManagedEvcs.ChannelId.SET_ENERGY_LIMIT).setNextValue(energyLimit);
				this.parent.logInfoInDebugmode(this.log, "Setting EVCS " + this.parent.alias()
						+ " Energy Limit in this Session to [" + energyLimit + " Wh]");

				// Prepare next write
				this.lastEnergySession = energyLimit;
			}
		}
	}
}
