package io.openems.edge.evcs.keba.kecontact;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		WriteChannel<Boolean> channel = this.parent.channel(KebaKeContact.ChannelId.SET_ENABLED);
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
			// calculate current based on phases and voltage. FIXME: this will have to be
			// adjusted if the EVCS is connected single phase
			Integer current = power / 3 /* 3 phases */ / 230 /* voltage */;
			if (!current.equals(this.lastCurrent) || this.nextCurrentWrite.isBefore(LocalDateTime.now())) {
				this.parent.logInfo(this.log,
						"Setting KEBA KeContact current to [" + current + " mA] - calculated from [" + power + " W]");
				boolean sentSuccessfully = parent.send("curr " + current);
				if (sentSuccessfully) {
					this.nextCurrentWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
					this.lastCurrent = current;
				}
			}
		}
	}
}
