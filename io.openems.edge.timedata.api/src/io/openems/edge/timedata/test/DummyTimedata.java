package io.openems.edge.timedata.test;

import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timedata.api.Timedata;

/**
 * Provides a simple, simulated {@link Timedata} component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyTimedata extends AbstractOpenemsComponent implements Timedata {

	private final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data = new TreeMap<>();

	public DummyTimedata(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Timedata.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	/**
	 * Adds a value to the Dummy Timedata.
	 *
	 * @param timestamp      the {@link ZonedDateTime}
	 * @param channelAddress the {@link ChannelAddress}
	 * @param value          the value as {@link Integer}
	 */
	public void add(ZonedDateTime timestamp, ChannelAddress channelAddress, Integer value) {
		this.add(timestamp, channelAddress, new JsonPrimitive(value));
	}

	/**
	 * Adds a value to the Dummy Timedata.
	 *
	 * @param timestamp      the {@link ZonedDateTime}
	 * @param channelAddress the {@link ChannelAddress}
	 * @param value          the value as {@link JsonElement}
	 */
	public void add(ZonedDateTime timestamp, ChannelAddress channelAddress, JsonElement value) {
		var perTime = this.data.get(timestamp);
		if (perTime == null) {
			perTime = new TreeMap<>();
			this.data.put(timestamp, perTime);
		}
		perTime.put(channelAddress, value);
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> result = new TreeMap<>();
		for (Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> entry : this.data.subMap(fromDate, toDate)
				.entrySet()) {
			SortedMap<ChannelAddress, JsonElement> subResult = new TreeMap<>();
			for (ChannelAddress channelAddress : channels) {
				subResult.put(channelAddress, entry.getValue().get(channelAddress));
			}
			result.put(entry.getKey(), subResult);
		}
		return result;
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		// TODO Auto-generated method stub
		throw new NotImplementedException("DummyTimedata.queryHistoricEnergy() is not implemented");
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		// TODO Auto-generated method stub
		throw new NotImplementedException("DummyTimedata.queryHistoricEnergyPerPeriod() is not implemented");
	}

	@Override
	public CompletableFuture<Optional<Object>> getLatestValue(ChannelAddress channelAddress) {

		var result = this.data.entrySet() //
				.stream() //
				.sorted((o1, o2) -> o2.getKey().compareTo(o1.getKey())).map(Entry::getValue) //
				.map(t -> t.get(channelAddress)) //
				.filter(Objects::nonNull) //
				.map(t -> (Object) t.getAsInt()) //
				.findFirst();

		return CompletableFuture.completedFuture(result);
	}
}
