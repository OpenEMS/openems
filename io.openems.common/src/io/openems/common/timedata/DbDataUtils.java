package io.openems.common.timedata;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.CollectorUtils;
import io.openems.common.utils.JsonUtils;

public final class DbDataUtils {

	private DbDataUtils() {
		super();
	}

	/**
	 * Normalizes the given table by adding null values for missing time stamps.
	 * 
	 * @param table      the data
	 * @param channels   the channels
	 * @param resolution the resolution
	 * @param fromDate   the starting date
	 * @param toDate     the end date
	 * @return the normalized table
	 */
	public static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> normalizeTable(//
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table, //
			Set<ChannelAddress> channels, //
			Resolution resolution, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate //
	) {
		if (table == null) {
			return null;
		}

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> normalizedTable = new TreeMap<>();

		var start = fromDate;
		while (start.isBefore(toDate)) {
			ZonedDateTime end = switch (resolution.getUnit()) {
			case CENTURIES, DECADES, ERAS, FOREVER, //
					HALF_DAYS, MICROS, MILLENNIA, //
					MILLIS, NANOS, WEEKS -> {
				// No specific handling required
				yield null;
			}
			case HOURS, MINUTES, SECONDS -> start.plus(resolution.getValue(), resolution.getUnit());
			case DAYS -> start.plusDays(resolution.getValue()) //
					.truncatedTo(DurationUnit.ofDays(1));
			case MONTHS -> start.plusMonths(resolution.getValue()) //
					.withDayOfMonth(1);
			case YEARS -> start.plusYears(resolution.getValue()) //
					.withDayOfYear(1);
			};

			if (end == null) {
				// not supported resolutions
				return table;
			}

			SortedMap<ChannelAddress, JsonElement> foundData = null;
			for (var data : table.entrySet()) {
				if (data.getKey().isBefore(start)) {
					continue;
				}
				// end exclusive
				if (data.getKey().isAfter(end)) {
					continue;
				}
				if (data.getKey().isEqual(end)) {
					continue;
				}
				foundData = data.getValue();
				break;
			}
			// fill with null values
			if (foundData == null) {
				foundData = channels.stream() //
						.collect(CollectorUtils.<ChannelAddress, ChannelAddress, JsonElement>//
								toSortedMap(Function.identity(), t -> JsonNull.INSTANCE));
			} else {
				foundData = fillUpMissingValues(foundData, channels);
			}
			normalizedTable.put(start, foundData);
			start = end;
		}

		return normalizedTable;
	}

	private static SortedMap<ChannelAddress, JsonElement> fillUpMissingValues(//
			SortedMap<ChannelAddress, JsonElement> data, //
			Set<ChannelAddress> channels //
	) {
		if (data.size() == channels.size()) {
			return data;
		}
		final var filledUpMap = new TreeMap<>(data);
		for (var channel : channels) {
			if (filledUpMap.containsKey(channel)) {
				continue;
			}
			filledUpMap.put(channel, JsonNull.INSTANCE);
		}
		return filledUpMap;
	}

	/**
	 * Calculates the difference of the every values based on the last valid value
	 * and drops all values which are before the fromDate.
	 * 
	 * @param data     the data
	 * @param fromDate the starting date
	 * @return the differences of the values
	 */
	public static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> calculateLastMinusFirst(//
			final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data, //
			final ZonedDateTime fromDate //
	) {
		final var lastValidValues = new TreeMap<ChannelAddress, JsonElement>();
		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> result = data.entrySet().stream() //
				.collect(Collectors.toMap(Entry::getKey, entry -> {
					if (entry.getValue() == null) {
						return Collections.emptySortedMap();
					}
					return entry.getValue().entrySet().stream() //
							.collect(Collectors.toMap(Entry::getKey, t -> {
								final var channel = t.getKey();
								final var value = t.getValue();
								final var lastValue = lastValidValues.get(channel);

								if (!JsonUtils.isNumber(value)) {
									return JsonNull.INSTANCE;
								}
								lastValidValues.put(channel, value);
								if (lastValue == null) {
									return value;
								}

								final var diff = value.getAsDouble() - lastValue.getAsDouble();
								if (diff < 0) {
									return JsonNull.INSTANCE;
								}
								return new JsonPrimitive(diff);
							}, (t, u) -> u, TreeMap::new));
				}, (t, u) -> u, TreeMap::new));

		result.headMap(fromDate).clear();
		return result;
	}

}
