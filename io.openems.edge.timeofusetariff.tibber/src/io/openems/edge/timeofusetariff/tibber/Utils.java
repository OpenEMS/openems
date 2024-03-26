package io.openems.edge.timeofusetariff.tibber;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.edge.timeofusetariff.tibber.TimeOfUseTariffTibberImpl.CLIENT_ERROR_CODE;
import static io.openems.edge.timeofusetariff.tibber.TimeOfUseTariffTibberImpl.TOO_MANY_REQUESTS_CODE;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.stream.Collectors.joining;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class Utils {

	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private static final int RETRY_AFTER_RATE_LIMIT_EXCEEDED_HOURS = 12;
	private static final int RETRY_AFTER_UNABLE_TO_UPDATE_PRICES_MINUTES = 5;

	private Utils() {

	}

	/**
	 * Parse the Tibber JSON to {@link TimeOfUsePrices}.
	 * 
	 * <p>
	 * If a filter is supplied, it is checked against 'id' (UUID) and 'appNickname'.
	 * If no filter is supplied, the method tries to be smart with finding the one
	 * unique correct result, i.e. it ignores empty/null objects.
	 *
	 * @param jsonData the Tibber JSON
	 * @param filter   filter for 'id' or 'appNickname'; null/blank for no filter
	 * @return the {@link TimeOfUsePrices}
	 * @throws OpenemsNamedException on error
	 */
	protected static TimeOfUsePrices parsePrices(String jsonData, String filter) throws OpenemsNamedException {
		var homes = getAsJsonArray(//
				getAsJsonObject(//
						getAsJsonObject(//
								parseToJsonObject(jsonData), //
								"data"), //
						"viewer"), //
				"homes");

		// Parse all homes
		TimeOfUsePrices result = null;
		OpenemsNamedException error = null;
		var successCount = 0;

		if (homes.size() == 1) {
			// If there's only one home, filter is set to null so that it is ignored while
			// parsing.
			filter = null;
		}

		for (JsonElement home : homes) {
			try {
				var subResult = parseHome(home, filter);
				if (subResult == null) {
					continue;
				}
				result = subResult;
				successCount++;
			} catch (OpenemsNamedException e) {
				error = e;
			}
		}

		// Evaluate result
		if (successCount < 1) {
			if (error != null) {
				throw error;
			} else if (homes.size() > 1 && filter != null && !filter.isBlank()) {
				throw new OpenemsException("Unable to parse any of multiple 'Homes'. Please check configured filter");
			} else {
				throw new OpenemsException("Unable to parse 'Home'");
			}

		} else if (successCount == 1) {
			// Parsed exactly one home successfully
			return result;

		} else {
			// successCount > 1
			LOG.warn("Found multiple 'Homes'. Please configure a specific Home 'ID' or 'appNickname' from: "
					+ homes.asList().stream() //
							.map(home -> getAsOptionalString(home, "id").orElse("") + ":"
									+ getAsOptionalString(home, "appNickname").orElse("")) //
							.collect(joining(", ")));
			throw new FoundMultipleHomesException();
		}
	}

	/**
	 * Parses one 'home'.
	 * 
	 * @param json   the 'home' JsonObject
	 * @param filter filter for 'id' or 'appNickname'; null/blank for no filter
	 * @return the parsed {@link TimeOfUsePrices}; null if filter does not match
	 * @throws OpenemsNamedException on parse error
	 */
	private static TimeOfUsePrices parseHome(JsonElement json, String filter) throws OpenemsNamedException {
		// Match filter
		if (filter != null && !filter.isBlank()) {
			var id = getAsString(json, "id"); //
			var appNickname = getAsOptionalString(json, "appNickname").orElse(""); //
			if (!filter.equals(id) && !filter.equals(appNickname)) {
				return null;
			}
		}

		var priceInfo = getAsJsonObject(//
				getAsJsonObject(//
						json, //
						"currentSubscription"), //
				"priceInfo");

		// Price info for today and tomorrow.
		var today = getAsJsonArray(priceInfo, "today");
		var tomorrow = getAsJsonArray(priceInfo, "tomorrow");

		// Adding to an array to avoid individual variables for individual for loops.
		JsonArray[] days = { today, tomorrow };

		var result = new TreeMap<ZonedDateTime, Double>();

		// parse the arrays for price and time stamps.
		for (var day : days) {
			for (var element : day) {
				// Multiply the price with 1000 to make it Currency/MWh.
				var price = getAsDouble(element, "total") * 1000;
				var startsAt = ZonedDateTime.parse(getAsString(element, "startsAt"), ISO_DATE_TIME)
						.withZoneSameInstant(ZoneId.systemDefault());

				// Adding the values in the Map.
				result.put(startsAt, price);
				result.put(startsAt.plusMinutes(15), price);
				result.put(startsAt.plusMinutes(30), price);
				result.put(startsAt.plusMinutes(45), price);
			}
		}
		return TimeOfUsePrices.from(result);
	}

	/**
	 * Calculates the delay until the next scheduled task run based on the HTTP
	 * status code and update availability.
	 * 
	 * @param httpStatusCode       The HTTP status code received from the request.
	 * @param filterIsRequired     A boolean indicating whether filter is required.
	 * @param unableToUpdatePrices A boolean indicating whether the prices couldn't
	 *                             be updated.
	 * @return The delay in seconds until the next scheduled task run.
	 */
	protected static long calculateDelay(int httpStatusCode, boolean filterIsRequired, boolean unableToUpdatePrices) {

		final var now = ZonedDateTime.now();
		final ZonedDateTime nextRun;

		if (!filterIsRequired && !unableToUpdatePrices) {
			// next price update at next hour for successful response
			nextRun = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
		} else if (httpStatusCode == TOO_MANY_REQUESTS_CODE) {
			LOG.warn("Rate limit exceeded. Retrying after " + RETRY_AFTER_RATE_LIMIT_EXCEEDED_HOURS + " hours.");
			nextRun = now.plusHours(RETRY_AFTER_RATE_LIMIT_EXCEEDED_HOURS);
		} else if (filterIsRequired || httpStatusCode == CLIENT_ERROR_CODE) {
			return 0;
		} else {
			nextRun = now.plusMinutes(RETRY_AFTER_UNABLE_TO_UPDATE_PRICES_MINUTES).truncatedTo(ChronoUnit.MINUTES);
			LOG.warn("Unable to Update the prices, Retrying again at: " + nextRun);
		}

		return Duration.between(now, nextRun.plusSeconds(new Random().nextInt(60))) // randomly add a few seconds
				.getSeconds();
	}

	/**
	 * Generate a GraphQL query.
	 * 
	 * @return a query string
	 */
	protected static String generateGraphQl() {
		return new StringBuilder() //
				.append("{\n") //
				.append("  viewer {\n") //
				.append("    homes {\n") //
				.append("      id\n") //
				.append("      appNickname\n") //
				.append("      currentSubscription{\n") //
				.append("        priceInfo{\n") //
				.append("          today {\n") //
				.append("            total\n") //
				.append("            startsAt\n") //
				.append("          }\n") //
				.append("          tomorrow {\n") //
				.append("            total\n") //
				.append("            startsAt\n") //
				.append("          }\n") //
				.append("        }\n") //
				.append("      }\n") //
				.append("    }\n") //
				.append("  }\n") //
				.append("}") //
				.toString();
	}
}
