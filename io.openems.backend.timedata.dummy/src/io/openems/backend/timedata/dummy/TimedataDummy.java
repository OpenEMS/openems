package io.openems.backend.timedata.dummy;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.edgewebsocket.EdgeCache;
import io.openems.backend.common.timedata.Timedata;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Timedata.Dummy", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimedataDummy extends AbstractOpenemsBackendComponent implements Timedata {

	private final Logger log = LoggerFactory.getLogger(TimedataDummy.class);
	private final Map<String, EdgeCache> edgeCacheMap = new HashMap<>();

	private Config config;

	public TimedataDummy() {
		super("Timedata.Dummy");
	}

	@Activate
	private void activate(Config config) throws OpenemsException {
		this.config = config;
		this.logInfo(this.log, "Activate");
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public void write(String edgeId, TimestampedDataNotification data) {
		// get existing or create new EdgeCache
		var edgeCache = this.edgeCacheMap.get(edgeId);
		if (edgeCache == null) {
			edgeCache = new EdgeCache();
			this.edgeCacheMap.put(edgeId, edgeCache);
		}

		// Update the Data Cache
		edgeCache.update(data.getData().rowMap());
	}

	@Override
	public void write(String edgeId, AggregatedDataNotification data) {
		this.logWarn(this.log, "Timedata.Dummy do not support write of AggregatedDataNotification");
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		this.logWarn(this.log, "I do not support querying historic data");
		return new TreeMap<>();
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		this.logWarn(this.log, "I do not support querying historic energy");
		return new TreeMap<>();
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		this.logWarn(this.log, "I do not support querying historic energy per period");
		return new TreeMap<>();
	}

	@Override
	public String id() {
		return this.config.id();
	}
}
