package io.openems.backend.common.metadata;

import java.time.ZonedDateTime;
import java.util.Objects;

import io.openems.common.channel.Level;
import io.openems.common.session.Role;

public class AlertingSetting {
    private final int id;
    private final String userId;
    private final Role userRole;

    private final ZonedDateTime offlineAlertLastNotification;
    private final ZonedDateTime sumStateAlertLastNotification;

    private final int offlineAlertDelayTime;
    private final int sumStateAlertDelayTime;

    private final Level sumStateAlertLevel;


    public AlertingSetting(int id, String userId, Role userRole, //
	    ZonedDateTime offlineAlertLastNotification, int offlineAlertDelayTime, //
	    ZonedDateTime sumStateAlertLastNotification, int sumStateAlertDelayTime, //
	    Level sumStateAlertLevel) {
	this.id = id;
	this.userId = userId;
	this.userRole = userRole;
	this.offlineAlertLastNotification = offlineAlertLastNotification;
	this.offlineAlertDelayTime = offlineAlertDelayTime;
	
	this.sumStateAlertLastNotification = sumStateAlertLastNotification;
	this.sumStateAlertDelayTime = sumStateAlertDelayTime;
	this.sumStateAlertLevel = sumStateAlertLevel;
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

    public ZonedDateTime getOfflineAlertLastNotification() {
	return this.offlineAlertLastNotification;
    }

    public int getOfflineAlertDelayTime() {
	return this.offlineAlertDelayTime;
    }

    public Level getSumStateAlertLevel() {
	return this.sumStateAlertLevel;
    }

    public int getSumStateAlertDelayTime() {
	return this.sumStateAlertDelayTime;
    }

    public ZonedDateTime getSumStateAlertLastNotification() {
	return this.sumStateAlertLastNotification;
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
	var other = (AlertingSetting) obj;
	return this.id == other.id;
    }
}
