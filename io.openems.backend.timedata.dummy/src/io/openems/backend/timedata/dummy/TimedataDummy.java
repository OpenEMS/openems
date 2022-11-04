package io.openems.backend.timedata.dummy;

import java.time.ZonedDateTime;
import java.util.Collections;
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

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.edgewebsocket.EdgeCache;
import io.openems.backend.common.timedata.Timedata;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Timedata.Dummy", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TimedataDummy extends AbstractOpenemsBackendComponent implements Timedata {

	private final Logger log = LoggerFactory.getLogger(TimedataDummy.class);
	private final Map<String, EdgeCache> edgeCacheMap = new HashMap<>();

	public TimedataDummy() {
		super("Timedata.Dummy");
	}

	@Activate
	private void activate(Config config) throws OpenemsException {
		this.logInfo(this.log, "Activate");
	}

	@Deactivate
	private void deactivate() {
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public Map<ChannelAddress, JsonElement> getChannelValues(String edgeId, Set<ChannelAddress> channelAddresses) {
		var edgeCache = this.edgeCacheMap.get(edgeId);
		if (edgeCache == null) {
			return Collections.emptyMap();
		}
		var result = new HashMap<ChannelAddress, JsonElement>();
		for (var channelAddress : channelAddresses) {
			result.put(channelAddress, edgeCache.getChannelValue(channelAddress));
		}
		return result;
	}

	@Override
	public void write(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) throws OpenemsException {
		// get existing or create new EdgeCache
		var edgeCache = this.edgeCacheMap.get(edgeId);
		if (edgeCache == null) {
			edgeCache = new EdgeCache();
			this.edgeCacheMap.put(edgeId, edgeCache);
		}

		// Complement incoming data with data from Cache, because only changed values
		// are transmitted
		edgeCache.complementDataFromCache(data.rowMap(), //
				(incomingTimestamp, cacheTimestamp) -> this.log.info(//
						"Edge [" + edgeId + "]: invalidate cache. " //
								+ "Incoming [" + incomingTimestamp + "]. " //
								+ "Cache [" + cacheTimestamp + "]"));
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

}
