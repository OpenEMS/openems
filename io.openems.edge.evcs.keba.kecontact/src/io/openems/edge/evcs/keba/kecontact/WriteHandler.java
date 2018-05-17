package io.openems.edge.evcs.keba.kecontact;

import java.time.LocalDateTime;
import java.util.Optional;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.evcs.keba.kecontact.KebaKeContact.ChannelId;

/**
 * Handles writes. Called in every cycle
 */
public class WriteHandler implements Runnable {

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
		this.setCurrent();
	}

	private String lastDisplay = null;
	private LocalDateTime nextDisplayWrite = LocalDateTime.MIN;

	/**
	 * Sets the display text from SET_DISPLAY channel
	 */
	private void setDisplay() {
		WriteChannel<String> channel = this.parent.channel(ChannelId.SET_DISPLAY);
		Optional<String> valueOpt = channel.value().asOptional();
		if (valueOpt.isPresent()) {
			String display = valueOpt.get();
			if (display.length() > 23) {
				display = display.substring(0, 23);
			}
			display = display.replace(" ", "$"); // $ == blank
			if (!display.equals(this.lastDisplay) || this.nextDisplayWrite.isBefore(LocalDateTime.now())) {
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
		WriteChannel<Boolean> channel = this.parent.channel(ChannelId.SET_ENABLED);
		Optional<Boolean> valueOpt = channel.value().asOptional();
		if (valueOpt.isPresent()) {
			Boolean enabled = valueOpt.get();
			if (!enabled.equals(this.lastEnabled) || this.nextEnabledWrite.isBefore(LocalDateTime.now())) {
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
	 * Sets the current from SET_CURRENT channel
	 */
	private void setCurrent() {
		WriteChannel<Integer> channel = this.parent.channel(ChannelId.SET_CURRENT);
		Optional<Integer> valueOpt = channel.value().asOptional();
		if (valueOpt.isPresent()) {
			Integer current = valueOpt.get();
			if (!current.equals(this.lastCurrent) || this.nextCurrentWrite.isBefore(LocalDateTime.now())) {
				boolean sentSuccessfully = parent.send("curr " + current);
				if (sentSuccessfully) {
					this.nextCurrentWrite = LocalDateTime.now().plusSeconds(WRITE_INTERVAL_SECONDS);
					this.lastCurrent = current;
				}
			}
		}
	}
}
