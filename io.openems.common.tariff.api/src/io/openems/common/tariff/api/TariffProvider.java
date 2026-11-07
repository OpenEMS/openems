package io.openems.common.tariff.api;

import java.util.List;

import io.openems.common.utils.BehaviorSubject;

public interface TariffProvider {
	/**
	 * Provides observable information about currently loaded tariffs.
	 *
	 * @return a {@link BehaviorSubject} that emits the current list of loaded
	 *         tariff infos and subsequent updates
	 */
	BehaviorSubject<List<LoadedTariffInfo>> getInfosAboutLoadedTariffs();

	/**
	 * Gets the provider ranking used for prioritization.
	 *
	 * @return the ranking value; higher values indicate higher priority
	 */
	int getRanking();

	/**
	 * Describes a snapshot or a loaded tariff together with the result of its last
	 * update attempt.
	 *
	 * @param tariff           the loaded tariff
	 * @param lastUpdateStatus the status of the last update
	 */
	record LoadedTariffInfo(Tariff tariff, TariffUpdateStatus lastUpdateStatus) {
	}
}
