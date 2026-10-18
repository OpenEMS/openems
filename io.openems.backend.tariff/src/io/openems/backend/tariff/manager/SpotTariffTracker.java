package io.openems.backend.tariff.manager;

import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;

public interface SpotTariffTracker {

	/**
	 * Returns a BehaviorSubject containing the tariff data for the bidding zone.
	 * The tariff data is updated whenever a new price is received from the provider
	 * or when the exchange rates change.
	 *
	 * @return BehaviorSubject with tariff data
	 */
	BehaviorSubject<TariffData> getTariffData();
}
