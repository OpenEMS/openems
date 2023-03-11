package io.openems.backend.core.timedatamanager;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.timedata.Timedata;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Core.TimedataManager", //
		immediate = true //
)
public class TimedataManagerImpl extends AbstractOpenemsBackendComponent implements TimedataManager {

	private final Logger log = LoggerFactory.getLogger(TimedataManagerImpl.class);

	private final List<String> _configTimedataIds;
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
		// TODO add JUnit test
		synchronized (this._rawTimedatas) {
			this.timedatas.set(ImmutableSortedSet.copyOf((t1, t2) -> {
				var idxT1 = this._configTimedataIds.indexOf(t1.id());
				var idxT2 = this._configTimedataIds.indexOf(t2.id());
				if (idxT1 != -1 && idxT2 != -1) {
					// Both services are mentioned in config: sort as defined
					return Integer.compare(idxT1, idxT2);
				} else if (idxT1 != -1) {
					// Only t1 is configured
					return -1;
				} else if (idxT2 != -1) {
					// Only t2 is configured
					return 1;
				}
				if (t1.id() != null && t2.id() != null) {
					// None is configured; ids are available
					var result = t1.id().compareTo(t2.id());
					if (result != 0) {
						// ids are different
						return result;
					}
				}
				// None is configured; ids are not available
				return t1.getClass().getSimpleName().compareTo(t2.getClass().getSimpleName());
			}, this._rawTimedatas));
		}
	}

	@Activate
	public TimedataManagerImpl(Config config) {
		super("Core.TimedataManager");
		this._configTimedataIds = Arrays.asList(config.timedata_ids());
		this.updateSortedTimedatas();
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		for (var timedata : this.timedatas.get()) {
			var data = timedata.queryHistoricData(edgeId, fromDate, toDate, channels, resolution);
			if (data != null) {
				return data;
			}
		}
		// no result
		this.logWarn(this.log, "No timedata result for 'queryHistoricData' on Edge=" + edgeId + "; FromDate=" + fromDate
				+ "; ToDate=" + toDate + "; Channels=" + channels + "; Resolution=" + resolution);
		return null;
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		for (var timedata : this.timedatas.get()) {
			var data = timedata.queryHistoricEnergy(edgeId, fromDate, toDate, channels);
			if (data != null) {
				return data;
			}
		}
		// no result
		this.logWarn(this.log, "No timedata result for 'queryHistoricEnergy' on Edge=" + edgeId + "; FromDate="
				+ fromDate + "; ToDate=" + toDate + "; Channels=" + channels);
		return null;
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		for (var timedata : this.timedatas.get()) {
			var data = timedata.queryHistoricEnergyPerPeriod(edgeId, fromDate, toDate, channels, resolution);
			if (data != null) {
				return data;
			}
		}
		// no result
		this.logWarn(this.log, "No timedata result for 'queryHistoricEnergyPerPeriod' on Edge=" + edgeId + "; FromDate="
				+ fromDate + "; ToDate=" + toDate + "; Channels=" + channels + "; Resolution=" + resolution);
		return null;
	}

	@Override
	public void write(String edgeId, TreeBasedTable<Long, String, JsonElement> data) {
		for (var timedata : this.timedatas.get()) {
			try {
				timedata.write(edgeId, data);
			} catch (OpenemsException e) {
				this.logWarn(this.log, "Timedata write failed for Edge=" + edgeId);
			}
		}
	}

}
