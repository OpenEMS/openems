package io.openems.edge.common.currency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrencyUtils {

	private static final Logger LOG = LoggerFactory.getLogger(CurrencyUtils.class);

	/**
	 * Get {@link Currency} for given code of the country. If the country code does
	 * not exist, {@link Currency#EUR} is returned as default. The given code is
	 * removed from all leading and trailing white spaces and converts all
	 * characters to upper case.
	 * 
	 * <p>
	 * Applies to Tibber.
	 * 
	 * @param countryCode The country code from the Json data retrieved from the
	 *                    API.
	 * @return Currency
	 */
	public static Currency getCurrencyFromCountryCode(String countryCode) {

		// Example: 'DE','SE'..
		var value = countryCode.trim().toUpperCase();

		switch (value) {
		case "DE":
			return Currency.EUR;
		case "SE":
			return Currency.SEK;
		case "US":
			return Currency.USD;
		default:
			return Currency.DEFAULT;
		}
	}

	/**
	 * Get {@link Currency} for given unit of the currency. If the currency unit
	 * does not exist, {@link Currency#DEFAULT} is returned as default. The given
	 * unit is removed from all leading and trailing white spaces and converts all
	 * characters to upper case.
	 * 
	 * <p>
	 * Applies to aWATTar and Corrently.
	 * 
	 * @param currencyUnit The element from the Json data retrieved from the API.
	 * @return Currency
	 */
	public static Currency getCurrencyFromCurrencyCode(String currencyUnit) {
		// Split the unit to get only currency.
		// example: 'EUR/MWh' -> 'EUR'
		var value = currencyUnit.split("/")[0].trim().toUpperCase();

		try {
			return Currency.valueOf(value);
		} catch (IllegalArgumentException e) {
			LOG.warn("Currency [" + currencyUnit + "] is not supported");
			return Currency.DEFAULT;
		}
	}

}
