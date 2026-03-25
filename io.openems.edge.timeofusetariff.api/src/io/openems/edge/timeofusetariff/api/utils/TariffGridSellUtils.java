package io.openems.edge.timeofusetariff.api.utils;

import java.util.Locale;

import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TariffGridSell;

public class TariffGridSellUtils {

	/**
	 * Generates a default DebugLog message for {@link TariffGridSell}
	 * implementations.
	 *
	 * @param tariff   the {@link TariffGridSell}
	 * @param currency the Currency (from {@link Meta} component)
	 * @return a debug log String
	 */
	public static String generateDebugLog(TariffGridSell tariff, Currency currency) {
		var result = new StringBuilder() //
				.append("GridSellPrice:"); //
		{
			var p = tariff.getGridSellPrices().getFirst();
			if (p != null) {
				result.append(String.format(Locale.ENGLISH, "%.4f", p / 1000));
			} else {
				result.append("-");
			}
		}
		if (!currency.isUndefined()) {
			result //
					.append(" ") //
					.append(currency.getName()) //
					.append("/kWh");
		}
		return result.toString();
	}
}
