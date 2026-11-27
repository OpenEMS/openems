package io.openems.edge.timeofusetariff.api;

import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;

public interface SpotTariffTracker {

	/**
	 * Returns the current tariff data. The tariff data is updated whenever new data
	 * is available.
	 *
	 * @return Tariff data, can be null.
	 */
	BehaviorSubject<TariffData> getTariffData();

	/**
	 * Checks whatever providers are available. If no providers are available, the
	 * tracker will not be able to provide any data.
	 *
	 * @return true if providers are available, false otherwise.
	 */
	boolean areProvidersAvailable();

}
