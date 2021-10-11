package io.openems.edge.controller.ess.timeofusetariff.discharge.tariff;

import java.time.ZonedDateTime;
import java.util.TreeMap;

public interface TimeOfUseTariff {

	/**
	 * This method returns the Quarterly prices in a {@link TreeMap} format.
	 * 
	 * @return prices {@link TreeMap} consisting of quarterly electricity prices
	 *         along with time; or an empty Map on error
	 */
	public TreeMap<ZonedDateTime, Float> getPrices();

}
