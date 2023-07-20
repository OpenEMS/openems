package io.openems.backend.common.alerting;

import java.time.ZonedDateTime;

public record SumStateAlertingSetting(int edgeOdooId, int userOdooId, int faultDelay, int warningDelay, ZonedDateTime lastNotification) {

}
