package io.openems.backend.tariff.manager;

import java.util.List;

import io.openems.common.event.EventEmitter;
import io.openems.common.types.BiddingZone;

public interface BackendTariffManager {

	/**
	 * Returns the spot tariff for the given bidding zone or null if there is no
	 * provider for the given bidding zone.
	 *
	 * @param biddingZone the bidding zone
	 * @return the spot tariff tracker for the bidding zone or null
	 */
	SpotTariffTracker getSpotTariff(BiddingZone biddingZone);

	/**
	 * Returns an event emitter that is triggered whenever the available spot tariff
	 * providers change.
	 *
	 * @return the event emitter for spot tariff provider changes
	 */
	EventEmitter<Void> getOnSpotTariffProvidersChange();

	/**
	 * Returns all bidding zones for which tariff providers are available.
	 *
	 * @return a list of all supported bidding zones
	 */
	List<BiddingZone> getAllSupportedBiddingZones();
}
