package io.openems.edge.predictor.lstmmodel.utilities;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;

public class DataUtility {

	/**
	 * Extracts data values.
	 *
	 * @param queryResult The SortedMap queryResult.
	 * @return An ArrayList of Double values extracted from non-null JsonElement
	 *         values.
	 */
	public static ArrayList<Double> getData(
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult) {

		ArrayList<Double> data = new ArrayList<>();

		queryResult.values().stream()//
				.map(SortedMap::values)//
				.flatMap(Collection::stream)//
				.map(v -> {
					if (v.isJsonNull()) {
						return null;
					}
					return v.getAsDouble();
				}).forEach(value -> data.add(value));

		// TODO remove this later
		if (isAllNulls(data)) {
			System.out.println("Data is all null, use a different predictor");
		}

		return data;
	}

	/**
	 * Checks if all elements in an ArrayList are null.
	 *
	 * @param array The ArrayList to be checked.
	 * @return true if all elements in the ArrayList are null, false otherwise.
	 */
	private static boolean isAllNulls(ArrayList<Double> array) {
		return array.stream().allMatch(Objects::isNull);
	}

	/**
	 * Combines trend and seasonality predictions into a single list of values.
	 *
	 * @param trendPrediction       The list of predicted trend values.
	 * @param seasonalityPrediction The list of predicted seasonality values.
	 * @return A combined list containing both trend and seasonality predictions.
	 * 
	 */
	public static ArrayList<Double> combine(ArrayList<Double> trendPrediction,
			ArrayList<Double> seasonalityPrediction) {

		for (int l = 0; l < trendPrediction.size(); l++) {
			seasonalityPrediction.set(l, trendPrediction.get(l));
		}
		return seasonalityPrediction;
	}

	/**
	 * Extracts OffsetDateTime objects from the keys of a SortedMap containing
	 * ZonedDateTime keys.
	 *
	 * @param queryResult The SortedMap containing ZonedDateTime keys and associated
	 *                    data.
	 * @return An ArrayList of OffsetDateTime objects extracted from the
	 *         ZonedDateTime keys.
	 */
	public static ArrayList<OffsetDateTime> getDate(
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult) {
		return queryResult.keySet().stream()//
				.map(ZonedDateTime::toOffsetDateTime)//
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Get minute.
	 * 
	 * @param nowDate         the now date
	 * @param hyperParameters the hyperparameter
	 * @return int minute
	 */
	public static Integer getMinute(ZonedDateTime nowDate, HyperParameters hyperParameters) {
		int interval = hyperParameters.getInterval();
		int minute = nowDate.getMinute();
		return (int) (minute / interval) * interval;
	}

}
