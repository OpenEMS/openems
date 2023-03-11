package io.openems.edge.timeofusetariff.tibber;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class Utils {

	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private Utils() {

	}

	/**
	 * Parse the Tibber JSON to the Price Map.
	 * 
	 * <p>
	 * If a filter is supplied, it is checked against 'id' (UUID) and 'appNickname'.
	 * If no filter is supplied, the method tries to be smart with finding the one
	 * unique correct result, i.e. it ignores empty/null objects.
	 *
	 * @param jsonData the Tibber JSON
	 * @param filter   filter for 'id' or 'appNickname'; null/blank for no filter
	 * @return the Price Map
	 * @throws OpenemsNamedException on error
	 */
	protected static ImmutableSortedMap<ZonedDateTime, Float> parsePrices(String jsonData, String filter)
			throws OpenemsNamedException {
		var line = JsonUtils.parseToJsonObject(jsonData);
		var homes = JsonUtils.getAsJsonObject(line, "data") //
				.getAsJsonObject("viewer") //
				.getAsJsonArray("homes");

		// Parse all homes
		SortedMap<ZonedDateTime, Float> result = null;
		OpenemsNamedException error = null;
		var successCount = 0;
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
			return ImmutableSortedMap.copyOf(result);

		} else {
			// successCount > 1
			LOG.warn("Found multiple 'Homes'. Please configure a specific Home 'ID' or 'appNickname' from: "
					+ homes.asList().stream() //
							.map(home -> JsonUtils.getAsOptionalString(home, "id").orElse("") + ":"
									+ JsonUtils.getAsOptionalString(home, "appNickname").orElse("")) //
							.collect(Collectors.joining(", ")));
			throw new FoundMultipleHomesException();
		}
	}

	/**
	 * Parses one 'home'.
	 * 
	 * @param json   the 'home' JsonObject
	 * @param filter filter for 'id' or 'appNickname'; null/blank for no filter
	 * @return the parsed prices; null if filter does not match
	 * @throws OpenemsNamedException on parse error
	 */
	private static SortedMap<ZonedDateTime, Float> parseHome(JsonElement json, String filter)
			throws OpenemsNamedException {
		// Match filter
		if (filter != null && !filter.isBlank()) {
			var id = JsonUtils.getAsString(json, "id"); //
			var appNickname = JsonUtils.getAsOptionalString(json, "appNickname").orElse(""); //
			if (!filter.equals(id) && !filter.equals(appNickname)) {
				return null;
			}
		}

		var priceInfo = JsonUtils.getAsJsonObject(json, "currentSubscription") //
				.getAsJsonObject("priceInfo");

		// Price info for today and tomorrow.
		var today = JsonUtils.getAsJsonArray(priceInfo, "today");
		var tomorrow = JsonUtils.getAsJsonArray(priceInfo, "tomorrow");

		// Adding to an array to avoid individual variables for individual for loops.
		JsonArray[] days = { today, tomorrow };

		var result = new TreeMap<ZonedDateTime, Float>();

		// parse the arrays for price and time stamps.
		for (JsonArray day : days) {
			for (JsonElement element : day) {
				// Multiply the price with 1000 to make it EUR/MWh.
				var marketPrice = JsonUtils.getAsFloat(element, "total") * 1000;
				var startTimeStamp = ZonedDateTime
						.parse(JsonUtils.getAsString(element, "startsAt"), DateTimeFormatter.ISO_DATE_TIME)
						.withZoneSameInstant(ZoneId.systemDefault());

				// Adding the values in the Map.
				result.put(startTimeStamp, marketPrice);
				result.put(startTimeStamp.plusMinutes(15), marketPrice);
				result.put(startTimeStamp.plusMinutes(30), marketPrice);
				result.put(startTimeStamp.plusMinutes(45), marketPrice);
			}
		}
		return result;
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
