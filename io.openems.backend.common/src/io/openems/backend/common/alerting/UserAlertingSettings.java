package io.openems.backend.common.alerting;

import java.time.ZonedDateTime;

public record UserAlertingSettings(String deviceId, String userLogin, //
		int edgeOfflineDelay, int edgeFaultDelay, int edgeWarningDelay, //
		ZonedDateTime lastEdgeOfflineNotification, ZonedDateTime lastSumStateNotification) {
	public UserAlertingSettings(String userLogin, int offlineEdgeDelay, int faultStateDelay, int warningStateDelay) {
		this(null, userLogin, offlineEdgeDelay, faultStateDelay, warningStateDelay, null, null);
	}
	
	public UserAlertingSettings(String deviceId, String userLogin) {
		this(deviceId,userLogin, 1440, 1440, 1440, null, null);
	}
}