package io.openems.backend.timedata.aggregatedinflux;

import static java.util.stream.Collectors.toSet;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.edge.EdgeManager;
import io.openems.backend.common.timedata.InternalTimedataException;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.DbDataUtils;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.CollectorUtils;
import io.openems.common.utils.JsonUtils;

@Component(//
		service = { QueryWithCurrentData.class } //
)
public class QueryWithCurrentData {

	private final TimedataManager timedataManager;
	private final EdgeManager edgeManager;
	private final Clock clock;

	public QueryWithCurrentData(//
			TimedataManager timedataManager, //
			EdgeManager edgeManager, //
			Clock clock //
	) {
		super();
		this.timedataManager = timedataManager;
		this.edgeManager = edgeManager;
		this.clock = clock;
	}

	@Activate
	public QueryWithCurrentData(//
			@Reference TimedataManager timedataManager, //
			@Reference EdgeManager edgeManager //
	) {
		this(timedataManager, edgeManager, Clock.systemDefaultZone());
	}

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
		final var channelValues = this.edgeManager.getChannelValues(edgeId, channels);
		final var availableChannels = channels.stream().filter(t -> {
			final var value = channelValues.get(t);
			return value != null && !value.isJsonNull();
		}).collect(toSet());

		if (availableChannels.isEmpty()) {
			throw new InternalTimedataException("Missing current values for edge[" + edgeId + "] for channels: "
					+ channels.stream() //
							.map(ChannelAddress::toString) //
							.collect(Collectors.joining(", ")));
		}

		SortedMap<ChannelAddress, JsonElement> previousValuesTemp;
		try {
			previousValuesTemp = this.timedataManager.queryFirstValueBefore(edgeId, fromDate, availableChannels);
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
		final var channelValues = this.edgeManager.getChannelValues(edgeId, channels);
		final var availableChannels = channels.stream().filter(t -> {
			final var value = channelValues.get(t);
			return value != null && !value.isJsonNull();
		}).collect(toSet());

		if (availableChannels.isEmpty()) {
			throw new InternalTimedataException("Missing current values for edge[" + edgeId + "] for channels: "
					+ channels.stream() //
							.map(ChannelAddress::toString) //
							.collect(Collectors.joining(", ")));
		}

		// add current values
		var now = ZonedDateTime.now(this.clock) //
				.withZoneSameInstant(fromDate.getZone()) //
				.truncatedTo(DurationUnit.ofDays(1));

		now = switch (resolution.getUnit()) {
		case MONTHS -> now.withDayOfMonth(1);
		case YEARS -> now.withDayOfMonth(1) //
				.withDayOfYear(1);
		default -> now;
		};

		if (channels.size() != availableChannels.size()) {
			rawExistingData = rawExistingData.entrySet().stream().collect(CollectorUtils
					.<Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>, ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>//
					toSortedMap(Entry::getKey, entry -> entry.getValue().entrySet().stream() //
							.filter(t -> availableChannels.contains(t.getKey())) //
							.collect(CollectorUtils.<Entry<ChannelAddress, JsonElement>, ChannelAddress, JsonElement>//
									toSortedMap(Entry::getKey, Entry::getValue, (t, u) -> t)),
							(t, u) -> t));
		}
		final var data = new TreeMap<>(rawExistingData);
		data.put(now, channelValues);

		final var result = DbDataUtils.calculateLastMinusFirst(data, fromDate);
		return DbDataUtils.normalizeTable(result, availableChannels, resolution, fromDate, toDate);
	}

}
