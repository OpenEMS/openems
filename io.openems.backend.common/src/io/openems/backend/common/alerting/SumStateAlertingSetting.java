package io.openems.backend.common.alerting;

import java.time.ZonedDateTime;

import io.openems.common.channel.Level;

public record SumStateAlertingSetting(String edgeId, String userLogin, int faultDelay, int warningDelay,
		ZonedDateTime lastNotification) {
	/**
	 * Get the appropriate delay for the given SumState level .
	 *
	 * @param state to get delay for
	 * @return delay as integer
	 */
	public int getDelay(Level state) {
		return switch (state) {
		case OK:
		case INFO:
			yield 0;
		case FAULT:
			yield this.faultDelay;
		case WARNING:
			yield this.warningDelay;
		default:
			yield -1;
		};
	}
}
