package io.openems.common.tariff.api.spottariff;

import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.types.BiddingZone;

public interface SpotTariffProvider extends TariffProvider {
	/**
	 * Returns the spot tariff for the given bidding zone. Make sure to call
	 * {@link #ungetSpotTariff(SpotTariff)} when you are done using the returned
	 * tariff instance.
	 *
	 * @param biddingZone the bidding zone to resolve the tariff for
	 * @return the spot tariff
	 */
	SpotTariff getSpotTariff(BiddingZone biddingZone);

	/**
	 * Releases a previously obtained spot tariff instance.
	 *
	 * @param tariffObj the spot tariff to release
	 */
	void ungetSpotTariff(SpotTariff tariffObj);

	/**
	 * Checks whether spot tariffs are supported for the given bidding zone.
	 *
	 * @param biddingZone the bidding zone to check
	 * @return {@code true} if spot tariffs are supported; otherwise {@code false}
	 */
	boolean isSpotTariffSupported(BiddingZone biddingZone);
}
