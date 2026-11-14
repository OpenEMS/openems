package io.openems.backend.tariff.manager;

import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;

public interface BackendSpotTariffProvider extends SpotTariffProvider {
	/**
	 * Gets the bidding zones for which this provider can and should actively fetch
	 * spot-tariff data.
	 *
	 * @return the fetchable bidding zones
	 */
	BiddingZone[] getFetchableBiddingZones();
}
