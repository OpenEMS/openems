package io.openems.backend.common.metadata;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.google.common.base.Objects;

public class EdgeUser {

	private final int id;
	private final String userId;
	private final String edgeId;

	private int timeToWait;
	private ZonedDateTime lastNotification;

	public EdgeUser(int id, String edgeId, String userId, int timeToWait, ZonedDateTime lastNotification) {
		this.id = id;
		this.edgeId = edgeId;
		this.userId = userId;
		this.lastNotification = lastNotification;
		this.timeToWait = timeToWait;
	}

	public ZonedDateTime getLastNotification() {
		return this.lastNotification;
	}

	/**
	 * Get LastNotification in given timezone.
	 *
	 * @param zone timezone to convert to
	 * @return lastNotification in given timezone
	 */
	public ZonedDateTime getLastNotification(ZoneId zone) {
		if (this.lastNotification == null) {
			return null;
		} else {
			return this.lastNotification.withZoneSameInstant(zone);
		}
	}

	public void setLastNotification(ZonedDateTime lastNotification) {
		ZoneId utc = ZoneId.of("UTC");
		if (lastNotification.getZone().equals(utc)) {
			lastNotification = lastNotification.withZoneSameInstant(utc);
		}

		this.lastNotification = lastNotification;
	}

	public int getTimeToWait() {
		return this.timeToWait;
	}

	/**
	 * Sets timeToWait with given value.
	 *
	 * @param timeToWait new value
	 * @return true if value has changed
	 */
	public boolean setTimeToWait(int timeToWait) {
		if (!Objects.equal(this.timeToWait, timeToWait)) {
			this.timeToWait = timeToWait;
			return true;
		}
		return false;
	}

	public int getId() {
		return this.id;
	}

	public String getEdgeId() {
		return this.edgeId;
	}

	public String getUserId() {
		return this.userId;
	}

}
