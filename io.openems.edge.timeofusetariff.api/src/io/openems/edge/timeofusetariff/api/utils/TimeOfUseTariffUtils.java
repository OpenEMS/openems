package io.openems.edge.timeofusetariff.api.utils;

import java.util.Locale;

import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public class TimeOfUseTariffUtils {

	/**
	 * Generates a default DebugLog message for {@link TimeOfUseTariff}
	 * implementations.
	 * 
	 * @param tou      the {@link TimeOfUseTariff}
	 * @param currency the Currency (from {@link Meta} component)
	 * @return a debug log String
	 */
	public static String generateDebugLog(TimeOfUseTariff tou, Currency currency) {
		var result = new StringBuilder() //
				.append("Price:"); //
		{
			var p = tou.getPrices().getFirst();
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
