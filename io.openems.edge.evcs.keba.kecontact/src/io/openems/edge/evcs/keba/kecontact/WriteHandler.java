package io.openems.edge.evcs.keba.kecontact;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.evcs.api.Evcs;

/**
 * Handles writes. Called in every cycle
 */
public class WriteHandler implements Runnable {

	private final Logger log = LoggerFactory.getLogger(WriteHandler.class);

	private final KebaKeContact parent;

	/**
	 * Minimum pause between two consecutive writes
	 */
	private final static int WRITE_INTERVAL_SECONDS = 60; // 60 seconds

	public WriteHandler(KebaKeContact parent) {
		this.parent = parent;
	}

	@Override
	public void run() {
		this.setDisplay();
		this.setEnabled();
		this.setPower();
	}

	private String lastDisplay = null;
	private LocalDateTime nextDisplayWrite = LocalDateTime.MIN;

	/**
	 * Sets the display text from SET_DISPLAY channel
	 * 
	 * Note:
	 * <ul>
	 * <li>Maximum 23 ASCII characters can be used.
	 * <li>If you use the text 'kWh', it will be replaced with '???' (due to MID
	 * metering certification)
	 * </ul>
	 */
	private void setDisplay() {
		// FIXME this (and all other functions) should use
		// "channel.getNextWriteValueAndReset()"
		WriteChannel<String> channel = this.parent.channel(Evcs.ChannelId.SET_DISPLAY_TEXT);
		Optional<String> valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			String display = valueOpt.get();
			if (display.length() > 23) {
				display = display.substring(0, 23);
			}
			display = display.replace(" ", "$"); // $ == blank
			if (!display.equals(this.lastDisplay) || this.nextDisplayWrite.isBefore(LocalDateTime.now())) {
				this.parent.logInfo(this.log, "Setting KEBA KeContact display text to [" + display + "]");
				boolean sentSuccessfully = parent.send("display 0 0 0 0 " + display);
				if (sentSuccessfully) {
					this.nextDisplayWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
					this.lastDisplay = display;
				}
			}
		}
	}

	private Boolean lastEnabled = null;
	private LocalDateTime nextEnabledWrite = LocalDateTime.MIN;

	/**
	 * Sets the enabled state from SET_ENABLED channel
	 */
	private void setEnabled() {
		WriteChannel<Boolean> channel = this.parent.channel(KebaChannelId.SET_ENABLED);
		Optional<Boolean> valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {
			Boolean enabled = valueOpt.get();
			if (!enabled.equals(this.lastEnabled) || this.nextEnabledWrite.isBefore(LocalDateTime.now())) {
				this.parent.logInfo(this.log,
						"Setting KEBA KeContact state to [" + (enabled ? "enabled" : "disabled") + "]");
				boolean sentSuccessfully = parent.send("ena " + (enabled ? "1" : "0"));
				if (sentSuccessfully) {
					this.nextEnabledWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
					this.lastEnabled = enabled;
				}
			}
		}
	}

	private Integer lastCurrent = null;
	private LocalDateTime nextCurrentWrite = LocalDateTime.MIN;

	/**
	 * Sets the current from SET_CHARGE_POWER channel
	 * 
	 * Allowed loading current are between 6000mA and 63000mA. Invalid values are
	 * discarded and the default is set to 6000mA. The value is also depending on
	 * the DIP-switch settings and the used cable of the charging station.
	 */
	private void setPower() {
		WriteChannel<Integer> channel = this.parent.channel(Evcs.ChannelId.SET_CHARGE_POWER);
		Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();
		if (valueOpt.isPresent()) {

			Integer power = valueOpt.get();
			Channel<Integer> phases = this.parent.channel(KebaChannelId.PHASES);
			Integer current = power * 1000 / phases.value().orElse(3) /* e.g. 3 phases */ / 230 /* voltage */ ;

			if (!current.equals(this.lastCurrent) || this.nextCurrentWrite.isBefore(LocalDateTime.now())) {

				this.parent.logInfo(this.log, "Setting KEBA KeContact current to [" + current
						+ " A] - calculated from [" + power + " W] by " + phases.value().orElse(3) + " Phase");

				try {
					Channel<Integer> currPower = this.parent.channel(KebaChannelId.ACTUAL_POWER);
					this.parent.setDisplayText()
							.setNextWriteValue("Charging " + (currPower.value().orElse(0) / 1000) + "W");
				} catch (OpenemsException e) {
					e.printStackTrace();
				}

				this.parent.logInfo(this.log, "currtime " + current);
				boolean sentSuccessfully = parent.send("currtime " + current + " 1");
				if (sentSuccessfully) {
					this.nextCurrentWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
					this.lastCurrent = current;
				}
			}
		}
	}
}
