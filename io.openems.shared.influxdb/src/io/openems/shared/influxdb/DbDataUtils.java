package io.openems.shared.influxdb;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.timedata.DurationUnit;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
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

		// currently only works for days and months otherwise just return the table
		if (resolution.getUnit() != ChronoUnit.DAYS //
				&& resolution.getUnit() != ChronoUnit.MONTHS) {
			return table;
		}
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> normalizedTable = new TreeMap<>();

		var start = fromDate;
		while (start.isBefore(toDate)) {
			ZonedDateTime end = null;
			switch (resolution.getUnit()) {
			case CENTURIES:
			case DECADES:
			case ERAS:
			case FOREVER:
			case HALF_DAYS:
			case HOURS:
			case MICROS:
			case MILLENNIA:
			case MILLIS:
			case MINUTES:
			case NANOS:
			case SECONDS:
			case WEEKS:
			case YEARS:
				// No specific handling required
				break;
			case DAYS:
				end = start.plusDays(resolution.getValue()) //
						.truncatedTo(DurationUnit.ofDays(1));
				break;
			case MONTHS:
				end = start.plusMonths(resolution.getValue()) //
						.withDayOfMonth(1);
				break;
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
						.collect(Collectors.toMap(//
								t -> t, //
								t -> JsonNull.INSTANCE, //
								(oldValue, newValue) -> newValue, //
								TreeMap::new //
						));
			}
			normalizedTable.put(start, foundData);
			start = end;
		}

		return normalizedTable;
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
