package io.openems.backend.common.metadata;

import java.time.ZonedDateTime;
import java.util.Objects;

import io.openems.common.session.Role;

public class UserAlertingSettings {
	private final int id;
	private final String userId;
	private final Role userRole;
	private final ZonedDateTime lastNotification;
	private final int delayTime;

	public UserAlertingSettings(String userId, int delayTime) {
		this(0, userId, null, null, delayTime);
	}

	public UserAlertingSettings(int id, String userId, Role userRole, ZonedDateTime lastNotification, int delayTime) {
		this.id = id;
		this.userId = userId;
		this.userRole = userRole;
		this.lastNotification = lastNotification;
		this.delayTime = delayTime;
	}

	public int getId() {
		return this.id;
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

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		var other = (UserAlertingSettings) obj;
		return this.id == other.id;
	}
}
