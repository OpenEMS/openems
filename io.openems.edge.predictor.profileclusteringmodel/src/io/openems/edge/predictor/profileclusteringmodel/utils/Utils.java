package io.openems.edge.predictor.profileclusteringmodel.utils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.SortedMap;

import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public final class Utils {

	/**
	 * Converts raw channel data into a quarterly (15-minute interval) time series.
	 *
	 * @param rawChannelData the raw channel data
	 * @param channelAddress the channel to extract values from
	 * @param from           the start time (inclusive)
	 * @param to             the end time (exclusive)
	 * @return a {@link Series} of timestamps and corresponding values
	 */
	public static Series<ZonedDateTime> fromChannelDataToQuarterlySeries(//
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> rawChannelData, //
			ChannelAddress channelAddress, //
			ZonedDateTime from, //
			ZonedDateTime to) {
		var timestamps = new ArrayList<ZonedDateTime>();
		for (var ts = from; ts.isBefore(to); ts = ts.plusMinutes(15)) {
			timestamps.add(ts);
		}

		var values = timestamps.stream()//
				.map(ts -> { //
					var element = rawChannelData.get(ts);
					if (element == null //
							|| element.get(channelAddress) == null //
							|| element.get(channelAddress).isJsonNull()) {
						return Double.NaN;
					}
					return element.get(channelAddress).getAsDouble();
				}).toList();

		return new Series<>(timestamps, values);
	}

	private Utils() {
	}
}
