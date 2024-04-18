package io.openems.backend.common.alerting;

import java.time.ZonedDateTime;

public record OfflineEdgeAlertingSetting(String edgeId, String userLogin, int delay, ZonedDateTime lastNotification) {

}
