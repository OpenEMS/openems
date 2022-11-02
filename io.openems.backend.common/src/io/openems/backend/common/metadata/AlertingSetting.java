package io.openems.backend.common.metadata;

import java.time.ZonedDateTime;

import io.openems.common.session.Role;

public class AlertingSetting {
	private final String userId;
	private final Role userRole;
	private final ZonedDateTime lastNotification;
	private final int delayTime;

	public AlertingSetting(String userId, Role userRole, ZonedDateTime lastNotification, int delayTime) {
		this.userId = userId;
		this.userRole = userRole;
		this.lastNotification = lastNotification;
		this.delayTime = delayTime;
	}

	public String getUserId() {
		return this.userId;
	}

	public Role getUserRole() {
		return this.userRole;
	}

	public ZonedDateTime getLastNotification() {
		return this.lastNotification;
	}

	public int getDelayTime() {
		return this.delayTime;
	}
}
