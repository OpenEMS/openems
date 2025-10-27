package io.openems.backend.core.timedatamanager;

import static io.openems.common.utils.CollectorUtils.toSortedMap;
import static java.util.stream.Collectors.toMap;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;

import io.openems.backend.common.timedata.InternalTimedataException;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.core.timedatamanager.DummyTimedata.Builder.QueryFirstValueBeforeParams;
import io.openems.backend.core.timedatamanager.DummyTimedata.Builder.QueryHistoricDataParams;
import io.openems.backend.core.timedatamanager.DummyTimedata.Builder.QueryHistoricEnergyParams;
import io.openems.backend.core.timedatamanager.DummyTimedata.Builder.QueryHistoricEnergyPerPeriodParams;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.CollectorUtils;
import io.openems.common.utils.FunctionUtils;

/**
 * Dummy BackendTimedata implementation which can be customized base on the
 * needs of the methods.
 */
public class DummyTimedata implements Timedata {

	public static class Builder {

		public record QueryHistoricDataParams(String edgeId, ZonedDateTime fromDate, ZonedDateTime toDate,
				Set<ChannelAddress> channels, Resolution resolution) {

		}

		public record QueryHistoricEnergyParams(String edgeId, ZonedDateTime fromDate, ZonedDateTime toDate,
				Set<ChannelAddress> channels) {

		}

		public record QueryHistoricEnergyPerPeriodParams(String edgeId, ZonedDateTime fromDate, ZonedDateTime toDate,
				Set<ChannelAddress> channels, Resolution resolution) {

		}

		public record QueryFirstValueBeforeParams(String edgeId, ZonedDateTime date, Set<ChannelAddress> channels) {

		}

		private String id;
		private ThrowingFunction<QueryHistoricDataParams, //
				SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>, //
				OpenemsNamedException> queryHistoricData = FunctionUtils
						.alwaysThrow(new InternalTimedataException("Not implemented"));
		private ThrowingFunction<QueryHistoricEnergyParams, //
				SortedMap<ChannelAddress, JsonElement>, //
				OpenemsNamedException> queryHistoricEnergy = FunctionUtils
						.alwaysThrow(new InternalTimedataException("Not implemented"));
		private ThrowingFunction<QueryHistoricEnergyPerPeriodParams, //
				SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>, //
				OpenemsNamedException> queryHistoricEnergyPerPeriod = FunctionUtils
						.alwaysThrow(new InternalTimedataException("Not implemented"));
		private ThrowingFunction<QueryFirstValueBeforeParams, //
				SortedMap<ChannelAddress, JsonElement>, //
				OpenemsNamedException> queryFirstValueBefore = FunctionUtils
						.alwaysThrow(new InternalTimedataException("Not implemented"));

		private BiConsumer<String, TimestampedDataNotification> writeTimestampedData = FunctionUtils::doNothing;
		private BiConsumer<String, AggregatedDataNotification> writeAggregatedData = FunctionUtils::doNothing;
		private BiConsumer<String, ResendDataNotification> writeResendData = FunctionUtils::doNothing;

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setQueryHistoricData(
				ThrowingFunction<QueryHistoricDataParams, SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>, OpenemsNamedException> queryHistoricData) {
			this.queryHistoricData = queryHistoricData;
			return this;
		}

		public Builder setQueryHistoricDataFromPredefinedData(
				Map<ZonedDateTime, Map<ChannelAddress, JsonElement>> data) {
			return this.setQueryHistoricData(params -> data.entrySet().stream() //
					.filter(entry -> !entry.getKey().isBefore(params.fromDate())
							&& entry.getKey().isBefore(params.toDate())) //
					.filter(entry -> entry.getValue().keySet().stream().anyMatch(c -> params.channels().contains(c))) //
					.collect(CollectorUtils
							.<Entry<ZonedDateTime, Map<ChannelAddress, JsonElement>>, ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>//
							toSortedMap(Entry::getKey, e -> e.getValue().entrySet().stream() //
									.filter(t -> params.channels().contains(t.getKey())) //
									.collect(CollectorUtils
											.<Entry<ChannelAddress, JsonElement>, ChannelAddress, JsonElement>//
											toSortedMap(Entry::getKey, Entry::getValue)))));
		}

		public Builder setQueryHistoricEnergy(
				ThrowingFunction<QueryHistoricEnergyParams, SortedMap<ChannelAddress, JsonElement>, OpenemsNamedException> queryHistoricEnergy) {
			this.queryHistoricEnergy = queryHistoricEnergy;
			return this;
		}

		public Builder setQueryHistoricEnergyFromPredefinedData(
				Map<ZonedDateTime, Map<ChannelAddress, JsonElement>> data) {
			return this.setQueryHistoricEnergy(params -> data.entrySet().stream() //
					.filter(entry -> !entry.getKey().isBefore(params.fromDate())
							&& entry.getKey().isBefore(params.toDate())) //
					.filter(entry -> entry.getValue().keySet().stream().anyMatch(c -> params.channels().contains(c))) //
					.<Entry<ChannelAddress, JsonElement>>mapMulti((e, c) -> {
						for (var entry : e.getValue().entrySet()) {
							if (!params.channels().contains(entry.getKey())) {
								continue;
							}
							c.accept(entry);
						}
					}) //
					.collect(toSortedMap(Entry::getKey, Entry::getValue)));
		}

		public Builder setQueryHistoricEnergyPerPeriod(
				ThrowingFunction<QueryHistoricEnergyPerPeriodParams, SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>, OpenemsNamedException> queryHistoricEnergyPerPeriod) {
			this.queryHistoricEnergyPerPeriod = queryHistoricEnergyPerPeriod;
			return this;
		}

		public Builder setQueryHistoricEnergyPerPeriodFromPredefinedData(
				Map<ZonedDateTime, Map<ChannelAddress, JsonElement>> data) {
			return this.setQueryHistoricEnergyPerPeriod(params -> data.entrySet().stream() //
					.filter(entry -> !entry.getKey().isBefore(params.fromDate())
							&& entry.getKey().isBefore(params.toDate())) //
					.filter(entry -> entry.getValue().keySet().stream().anyMatch(c -> params.channels().contains(c))) //
					.collect(CollectorUtils
							.<Entry<ZonedDateTime, Map<ChannelAddress, JsonElement>>, ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>//
							toSortedMap(Entry::getKey, e -> e.getValue().entrySet().stream() //
									.filter(t -> params.channels().contains(t.getKey())) //
									.collect(toMap(Entry::getKey, Entry::getValue, (t, u) -> t, TreeMap::new)))));
		}

		public Builder setQueryFirstValueBefore(
				ThrowingFunction<QueryFirstValueBeforeParams, SortedMap<ChannelAddress, JsonElement>, OpenemsNamedException> queryFirstValueBefore) {
			this.queryFirstValueBefore = queryFirstValueBefore;
			return this;
		}

		public Builder setQueryFirstValueBeforeFromPredefinedData(
				Map<ZonedDateTime, Map<ChannelAddress, JsonElement>> data) {
			return this.setQueryFirstValueBefore(params -> data.entrySet().stream() //
					.filter(entry -> entry.getKey().isBefore(params.date())) //
					.filter(entry -> entry.getValue().keySet().stream() //
							.anyMatch(c -> params.channels().contains(c))) //
					.<Entry<ChannelAddress, JsonElement>>mapMulti((e, c) -> {
						for (var entry : e.getValue().entrySet()) {
							if (!params.channels().contains(entry.getKey())) {
								continue;
							}
							c.accept(entry);
						}
					}) //
					.collect(toSortedMap(Entry::getKey, Entry::getValue)));
		}

		public Builder setWriteTimestampedData(BiConsumer<String, TimestampedDataNotification> writeTimestampedData) {
			this.writeTimestampedData = writeTimestampedData;
			return this;
		}

		public Builder setWriteAggregatedData(BiConsumer<String, AggregatedDataNotification> writeAggregatedData) {
			this.writeAggregatedData = writeAggregatedData;
			return this;
		}

		public Builder setWriteResendData(BiConsumer<String, ResendDataNotification> writeResendData) {
			this.writeResendData = writeResendData;
			return this;
		}

		public Timedata build() {
			return new DummyTimedata(this.id, this.queryHistoricData, this.queryHistoricEnergy,
					this.queryHistoricEnergyPerPeriod, this.queryFirstValueBefore, this.writeTimestampedData,
					this.writeAggregatedData, this.writeResendData);
		}

	}

	/**
	 * Creates a {@link Builder} for a {@link Timedata}.
	 * 
	 * @return the {@link Builder}
	 */
	public static Builder create() {
		return new Builder();
	}

	private final String id;

	private final ThrowingFunction<QueryHistoricDataParams, //
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>, //
			OpenemsNamedException> queryHistoricData;
	private final ThrowingFunction<QueryHistoricEnergyParams, //
			SortedMap<ChannelAddress, JsonElement>, //
			OpenemsNamedException> queryHistoricEnergy;
	private final ThrowingFunction<QueryHistoricEnergyPerPeriodParams, //
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>, //
			OpenemsNamedException> queryHistoricEnergyPerPeriod;
	private final ThrowingFunction<QueryFirstValueBeforeParams, //
			SortedMap<ChannelAddress, JsonElement>, //
			OpenemsNamedException> queryFirstValueBefore;

	private final BiConsumer<String, TimestampedDataNotification> writeTimestampedData;
	private final BiConsumer<String, AggregatedDataNotification> writeAggregatedData;
	private final BiConsumer<String, ResendDataNotification> writeResendData;

	public DummyTimedata(String id,
			ThrowingFunction<QueryHistoricDataParams, SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>, OpenemsNamedException> queryHistoricData,
			ThrowingFunction<QueryHistoricEnergyParams, SortedMap<ChannelAddress, JsonElement>, OpenemsNamedException> queryHistoricEnergy,
			ThrowingFunction<QueryHistoricEnergyPerPeriodParams, SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>, OpenemsNamedException> queryHistoricEnergyPerPeriod,
			ThrowingFunction<QueryFirstValueBeforeParams, SortedMap<ChannelAddress, JsonElement>, OpenemsNamedException> queryFirstValueBefore,
			BiConsumer<String, TimestampedDataNotification> writeTimestampedData,
			BiConsumer<String, AggregatedDataNotification> writeAggregatedData,
			BiConsumer<String, ResendDataNotification> writeResendData) {
		super();
		this.id = id;
		this.queryHistoricData = queryHistoricData;
		this.queryHistoricEnergy = queryHistoricEnergy;
		this.queryHistoricEnergyPerPeriod = queryHistoricEnergyPerPeriod;
		this.queryFirstValueBefore = queryFirstValueBefore;
		this.writeTimestampedData = writeTimestampedData;
		this.writeAggregatedData = writeAggregatedData;
		this.writeResendData = writeResendData;
	}

	@Override
	public String id() {
		return this.id;
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		return this.queryHistoricData
				.apply(new QueryHistoricDataParams(edgeId, fromDate, toDate, channels, resolution));
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		return this.queryHistoricEnergy.apply(new QueryHistoricEnergyParams(edgeId, fromDate, toDate, channels));
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		return this.queryHistoricEnergyPerPeriod
				.apply(new QueryHistoricEnergyPerPeriodParams(edgeId, fromDate, toDate, channels, resolution));
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryFirstValueBefore(String edgeId, ZonedDateTime date,
			Set<ChannelAddress> channels) throws OpenemsNamedException {
		return this.queryFirstValueBefore.apply(new QueryFirstValueBeforeParams(edgeId, date, channels));
	}

	@Override
	public void write(String edgeId, TimestampedDataNotification data) {
		this.writeTimestampedData.accept(edgeId, data);
	}

	@Override
	public void write(String edgeId, AggregatedDataNotification data) {
		this.writeAggregatedData.accept(edgeId, data);
	}

	@Override
	public void write(String edgeId, ResendDataNotification data) {
		this.writeResendData.accept(edgeId, data);
	}

}
