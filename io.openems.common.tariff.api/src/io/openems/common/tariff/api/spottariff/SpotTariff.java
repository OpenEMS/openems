package io.openems.common.tariff.api.spottariff;

import io.openems.common.tariff.api.Tariff;
import io.openems.common.types.BiddingZone;

public interface SpotTariff extends Tariff {

	/**
	 * Gets the bidding zone this spot tariff applies to.
	 *
	 * @return the bidding zone
	 */
	BiddingZone getBiddingZone();
}
