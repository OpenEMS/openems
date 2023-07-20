package io.openems.backend.common.alerting;

import java.time.ZonedDateTime;

public record OfflineEdgeAlertingSetting(int edgeOdooId, int userOdooId, int delay, ZonedDateTime lastNotification) {

}
