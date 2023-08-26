package io.openems.backend.timedata.aggregatedinflux;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.edgewebsocket.EdgeWebsocket;
import io.openems.backend.common.timedata.InternalTimedataException;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.shared.influxdb.DbDataUtils;

@Component(//
		service = { QueryWithCurrentData.class } //
)
public class QueryWithCurrentData {

	@Reference
	private TimedataManager timedataManager;

	@Reference
	private EdgeWebsocket edgeWebsocket;

	/**
	 * {@link Timedata#queryHistoricEnergy(String, ZonedDateTime, ZonedDateTime, Set)}.
	 * 
	 * @param edgeId   the id of the edge
	 * @param fromDate the starting date
	 * @param toDate   the stop date
	 * @param channels the channels
	 * @return the values
	 * @throws OpenemsNamedException on error
	 */
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(//
			String edgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException {
		final var channelValues = this.edgeWebsocket.getChannelValues(edgeId, channels);
		var missingChannels = channelValues.entrySet().stream().filter(entry -> entry.getValue().isJsonNull())
				.map(Entry::getKey).toList();
		if (!missingChannels.isEmpty()) {
			throw new InternalTimedataException("Missing current values for edge[" + edgeId + "] for channels: "
					+ missingChannels.stream() //
							.map(ChannelAddress::toString) //
							.collect(Collectors.joining(", ")));
		}
		SortedMap<ChannelAddress, JsonElement> previousValuesTemp;
		try {
			previousValuesTemp = this.timedataManager.queryFirstValueBefore(edgeId, fromDate, channels);
		} catch (OpenemsNamedException e) {
			previousValuesTemp = Collections.emptySortedMap();
		}
		final SortedMap<ChannelAddress, JsonElement> previousValues = previousValuesTemp;
		return channelValues.entrySet().stream() //
				.collect(Collectors.toMap(Entry::getKey, t -> {
					var previousValue = previousValues.get(t.getKey());
					if (previousValue == null || previousValue.isJsonNull()) {
						previousValue = new JsonPrimitive(0);
					}
					final var currentValue = t.getValue();
					if (!JsonUtils.isNumber(previousValue)) {
						return currentValue;
					}
					if (!JsonUtils.isNumber(currentValue)) {
						return currentValue;
					}
					return new JsonPrimitive(currentValue.getAsDouble() - (previousValue.getAsDouble()));
				}, (t, u) -> u, TreeMap::new));
	}

	/**
	 * {@link Timedata#queryHistoricEnergyPerPeriod(String, ZonedDateTime, ZonedDateTime, Set, Resolution)}.
	 * 
	 * @param edgeId          the id of the edge
	 * @param fromDate        the starting date
	 * @param toDate          the stop date
	 * @param channels        the channels
	 * @param resolution      the resolution
	 * @param rawExistingData the already existing date which should be filled up
	 *                        with the current data
	 * @return the values
	 * @throws OpenemsNamedException on error
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(//
			String edgeId, //
			ZonedDateTime fromDate, //
			ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution, //
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> rawExistingData //
	) throws OpenemsNamedException {
		final var channelValues = this.edgeWebsocket.getChannelValues(edgeId, channels);
		var missingChannels = channelValues.entrySet().stream().filter(entry -> entry.getValue().isJsonNull())
				.map(Entry::getKey).toList();
		if (!missingChannels.isEmpty()) {
			throw new InternalTimedataException("Missing current values for edge[" + edgeId + "] for channels: "
					+ missingChannels.stream() //
							.map(ChannelAddress::toString) //
							.collect(Collectors.joining(", ")));
		}

		// add current values
		final var now = ZonedDateTime.now(fromDate.getZone()) //
				.truncatedTo(DurationUnit.ofDays(1));
		final var data = new TreeMap<>(rawExistingData);
		data.put(now, channelValues);

		final var result = DbDataUtils.calculateLastMinusFirst(data, fromDate);
		return DbDataUtils.normalizeTable(result, channels, resolution, fromDate, toDate);
	}

}
