package io.openems.backend.common.alerting;

import java.time.ZonedDateTime;

import io.openems.common.session.Role;

public record UserAlertingSettings(int userOdooId, int deviceOdooId, String userLogin, Role userRole, //
		int edgeOfflineDelay, int edgeFaultDelay, int edgeWarningDelay, //
		ZonedDateTime lastEdgeOfflineNotification, ZonedDateTime lastSumStateNotification) {
	public UserAlertingSettings(String userLogin, int offlineEdgeDelay, int faultStateDelay, int warningStateDelay) {
		this(-1, -1, userLogin, null, offlineEdgeDelay, faultStateDelay, warningStateDelay, null, null);
	}
}