package io.vev.backend.edge2kafka.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable representation of a consumption record as exchanged with external
 * services.
 *
 * <p>
 * All scalar values are nullable unless stated otherwise. The canonical
 * constructor enforces {@code startedAt} and {@code cumulatedConsumptionWh}
 * which correspond to the mandatory fields in the original schema.
 * </p>
 */
public record Consumption(
		Double instantWatts,
		Double instantWattsL1,
		Double instantWattsL2,
		Double instantWattsL3,
		Double instantAmps,
		Double instantAmpsL1,
		Double instantAmpsL2,
		Double instantAmpsL3,
		Double instantVolts,
		Double instantVoltsL1,
		Double instantVoltsL2,
		Double instantVoltsL3,
		Double consumptionWh,
		String id,
		Instant startedAt,
		Instant endedAt,
		Long transactionId,
		String chargeBoxID,
		Integer connectorId,
		String siteAreaID,
		String siteID,
		String assetID,
		Double cumulatedConsumptionWh,
		String pricingSource,
		Double amount,
		Double roundedAmount,
		Double cumulatedAmount,
		String currencyCode,
		Integer inactivitySecs,
		Integer totalInactivitySecs,
		Integer totalDurationSecs,
		Double stateOfCharge,
		String userID,
		Boolean toPrice,
		Double limitAmps,
		Double limitWatts,
//		ConnectorCurrentLimitSource limitSource,
		Double limitSiteAreaAmps,
		Double limitSiteAreaWatts,
//		SiteAreaLimitSource limitSiteAreaSource,
		Boolean smartChargingActive) {

	public Consumption {
		Objects.requireNonNull(startedAt, "startedAt");
		Objects.requireNonNull(cumulatedConsumptionWh, "cumulatedConsumptionWh");
	}
}

