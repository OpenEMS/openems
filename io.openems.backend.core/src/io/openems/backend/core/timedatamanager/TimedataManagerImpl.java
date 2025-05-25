package io.openems.backend.core.timedatamanager;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.debugcycle.MetricsConsumer;
import io.openems.backend.common.timedata.InternalTimedataException;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.function.ThrowingTriConsumer;
import io.openems.common.jsonrpc.notification.AbstractDataNotification;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.DbDataUtils;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.Tuple;
import io.openems.common.utils.ComparatorUtils;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Core.TimedataManager", //
		immediate = true //
)
public class TimedataManagerImpl extends AbstractOpenemsBackendComponent implements TimedataManager, MetricsConsumer {

	private interface QueryDataFunction<R> extends ThrowingBiFunction<Timedata, Set<ChannelAddress>, R, //
			OpenemsNamedException> {

	}

	private final Logger log = LoggerFactory.getLogger(TimedataManagerImpl.class);

	private List<String> _configTimedataIds;
	private final List<Timedata> _rawTimedatas = new ArrayList<>();
	private final AtomicReference<ImmutableSortedSet<Timedata>> timedatas = new AtomicReference<>(
			ImmutableSortedSet.of());

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE)
	protected synchronized void addTimedata(Timedata timedata) {
		synchronized (this._rawTimedatas) {
			this._rawTimedatas.add(timedata);
			this.updateSortedTimedatas();
		}
	}

	protected synchronized void removeTimedata(Timedata timedata) {
		synchronized (this._rawTimedatas) {
			this._rawTimedatas.remove(timedata);
			this.updateSortedTimedatas();
		}
	}

	private void updateSortedTimedatas() {
		synchronized (this._rawTimedatas) {
			this.timedatas.set(ImmutableSortedSet.copyOf(
					ComparatorUtils.comparatorIdList(this._configTimedataIds, Timedata::id), this._rawTimedatas));
		}
	}

	public TimedataManagerImpl() {
		super("Core.TimedataManager");
		this._configTimedataIds = Collections.emptyList();
	}

	/**
	 * Activates the component.
	 * 
	 * @param config the {@link Config Configuration}
	 */
	@Activate
	@Modified
	public void activate(Config config) {
		this._configTimedataIds = Arrays.asList(config.timedata_ids());
		this.updateSortedTimedatas();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * The {@link TimedataManager} implementation never returns null, but throws an
	 * Exception instead
	 */
	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(//
			String edgeId, //
			ZonedDateTime fromDate, ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsNamedException {

		final var resultData = this.queryTimestampedValues(channels,
				(t, c) -> t.queryHistoricData(edgeId, fromDate, toDate, c, resolution));

		if (resultData.isEmpty()) {
			this.logWarn(this.log, "No timedata result for 'queryHistoricData' on Edge=" + edgeId + "; FromDate="
					+ fromDate + "; ToDate=" + toDate + "; Channels=" + channels + "; Resolution=" + resolution);
			throw new OpenemsException("Unable to query historic data. Result is null");
		}

		return DbDataUtils.normalizeTable(resultData, channels, resolution, fromDate, toDate);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * The {@link TimedataManager} implementation never returns null, but throws an
	 * Exception instead
	 */
	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(//
			String edgeId, //
			ZonedDateTime fromDate, ZonedDateTime toDate, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException {

		final var resultData = this.querySingleValues(channels,
				(t, c) -> t.queryHistoricEnergy(edgeId, fromDate, toDate, c));

		if (resultData.isEmpty()) {
			// no result
			this.logWarn(this.log, "No timedata result for 'queryHistoricEnergy' on Edge=" + edgeId + "; FromDate="
					+ fromDate + "; ToDate=" + toDate + "; Channels=" + channels);
			throw new OpenemsException("Unable to query historic data. Result is null");
		}

		return resultData;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * The {@link TimedataManager} implementation never returns null, but throws an
	 * Exception instead
	 */
	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(//
			String edgeId, //
			ZonedDateTime fromDate, ZonedDateTime toDate, //
			Set<ChannelAddress> channels, //
			Resolution resolution //
	) throws OpenemsNamedException {

		final var resultData = this.queryTimestampedValues(channels,
				(t, c) -> t.queryHistoricEnergyPerPeriod(edgeId, fromDate, toDate, c, resolution));

		if (resultData.isEmpty()) {
			this.logWarn(this.log,
					"No timedata result for 'queryHistoricEnergyPerPeriod' on Edge=" + edgeId + "; FromDate=" + fromDate
							+ "; ToDate=" + toDate + "; Channels=" + channels + "; Resolution=" + resolution);
			throw new OpenemsException("Unable to query historic energy per period. Result is null");
		}

		return DbDataUtils.normalizeTable(resultData, channels, resolution, fromDate, toDate);
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryFirstValueBefore(//
			String edgeId, //
			ZonedDateTime date, //
			Set<ChannelAddress> channels //
	) throws OpenemsNamedException {

		final var resultData = this.querySingleValues(channels, (t, c) -> t.queryFirstValueBefore(edgeId, date, c));

		if (resultData.isEmpty()) {
			this.logWarn(this.log, "No timedata result for 'queryFirstValueBefore' on Edge=" + edgeId + "; Date=" + date
					+ "; Channels=" + channels);
			throw new OpenemsException("Unable to query first value before. Result is null");
		}

		return resultData;
	}

	private SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryTimestampedValues(//
			Set<ChannelAddress> channels, //
			QueryDataFunction<Map<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>> mapper //
	) throws OpenemsNamedException {

		final var resultData = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();
		final var missingChannels = new HashSet<>(channels);

		OpenemsNamedException potentialError = null;
		for (var result : this.iterateTimedataResults(t -> mapper.apply(t, missingChannels))) {
			final var values = result.a();
			final var error = result.b();
			if (values == null) {
				if (potentialError == null && error != null) {
					potentialError = error;
				}
				continue;
			}

			if (values == null || values.isEmpty()) {
				continue;
			}

			for (var entry : values.entrySet()) {
				missingChannels.removeAll(entry.getValue().keySet());

				final var existing = resultData.get(entry.getKey());
				if (existing == null) {
					resultData.put(entry.getKey(), entry.getValue());
					continue;
				}

				existing.putAll(entry.getValue());
			}

			if (missingChannels.isEmpty()) {
				return resultData;
			}

		}

		if (resultData.isEmpty() && potentialError != null) {
			throw potentialError;
		}

		return resultData;
	}

	private SortedMap<ChannelAddress, JsonElement> querySingleValues(//
			Set<ChannelAddress> channels, //
			QueryDataFunction<Map<ChannelAddress, JsonElement>> mapper //
	) throws OpenemsNamedException {

		final var resultData = new TreeMap<ChannelAddress, JsonElement>();
		final var missingChannels = new HashSet<>(channels);

		OpenemsNamedException potentialError = null;
		for (var result : this.iterateTimedataResults(t -> mapper.apply(t, missingChannels))) {
			final var values = result.a();
			final var error = result.b();
			if (values == null) {
				if (potentialError == null && error != null) {
					potentialError = error;
				}
				continue;
			}

			if (values == null || values.isEmpty()) {
				continue;
			}

			missingChannels.removeAll(values.keySet());
			resultData.putAll(values);

			if (missingChannels.isEmpty()) {
				return resultData;
			}
		}

		if (resultData.isEmpty()) {
			if (potentialError != null) {
				throw potentialError;
			}
			return resultData;
		}

		// fill up if at least one value is present
		for (var channel : missingChannels) {
			resultData.put(channel, JsonNull.INSTANCE);
		}

		return resultData;
	}

	@Override
	public void write(String edgeId, AggregatedDataNotification data) {
		this.write(edgeId, data, Timedata::write);
	}

	@Override
	public void write(String edgeId, TimestampedDataNotification data) {
		this.write(edgeId, data, Timedata::write);
	}

	@Override
	public void write(String edgeId, ResendDataNotification data) {
		this.write(edgeId, data, Timedata::write);
	}

	private <T extends AbstractDataNotification> void write(//
			final String edgeId, //
			final T data, //
			final ThrowingTriConsumer<Timedata, String, T, OpenemsException> method //
	) {
		for (var timedata : this.timedatas.get()) {
			try {
				method.accept(timedata, edgeId, data);
			} catch (OpenemsException e) {
				this.logWarn(this.log, "Timedata write failed for Edge=" + edgeId);
			}
		}
	}

	private <T> Iterable<Tuple<T, OpenemsNamedException>> iterateTimedataResults(
			ThrowingFunction<Timedata, T, OpenemsNamedException> mapper) {
		return () -> new Iterator<Tuple<T, OpenemsNamedException>>() {

			private final Iterator<Timedata> timedataIterator = TimedataManagerImpl.this.timedatas.get().iterator();

			@Override
			public boolean hasNext() {
				return this.timedataIterator.hasNext();
			}

			@Override
			public Tuple<T, OpenemsNamedException> next() {
				final var timedata = this.timedataIterator.next();
				try {
					return new Tuple<>(mapper.apply(timedata), null);
				} catch (InternalTimedataException e) {
					TimedataManagerImpl.this.log.info(timedata.id() + ": " + e.getMessage());
				} catch (OpenemsNamedException e) {
					TimedataManagerImpl.this.log.info(timedata.id() + ": " + e.getMessage());
					return new Tuple<>(null, e);
				} catch (RuntimeException e) {
					TimedataManagerImpl.this.log.info(timedata.id() + ": " + e.getMessage(), e);
				}
				return new Tuple<>(null, null);
			}

		};
	}

	@Override
	public void consumeMetrics(ZonedDateTime now, Map<String, JsonElement> metrics) {
		final var epochMillis = now.toInstant().toEpochMilli();
		final var data = TreeBasedTable.<Long, String, JsonElement>create();
		for (var entry : metrics.entrySet()) {
			data.put(epochMillis, entry.getKey(), entry.getValue());
		}
		this.write("backend0", new TimestampedDataNotification(data));
	}

}
