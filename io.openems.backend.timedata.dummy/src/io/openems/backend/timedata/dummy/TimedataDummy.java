package io.openems.backend.timedata.dummy;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
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
import io.openems.backend.timedata.api.EdgeCache;
import io.openems.backend.timedata.api.Timedata;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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
	void activate(Config config) throws OpenemsException {
		this.logInfo(this.log, "Activate");
	}

	@Deactivate
	void deactivate() {
		this.logInfo(this.log, "Deactivate");
	}

	public Optional<JsonElement> getChannelValue(String edgeId, ChannelAddress channelAddress) {
		EdgeCache edgeCache = this.edgeCacheMap.get(edgeId);
		if (edgeCache != null) {
			return edgeCache.getChannelValue(channelAddress);
		} else {
			return Optional.empty();
		}
	}

	@Override
	public void write(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) throws OpenemsException {
		// get existing or create new EdgeCache
		EdgeCache edgeCache = this.edgeCacheMap.get(edgeId);
		if (edgeCache == null) {
			edgeCache = new EdgeCache();
			this.edgeCacheMap.put(edgeId, edgeCache);
		}

		// Prepare data table. Takes entries starting with eldest timestamp (ascending
		// order)
		for (Entry<Long, Map<ChannelAddress, JsonElement>> dataEntry : data.rowMap().entrySet()) {
			Long timestamp = dataEntry.getKey();
			Map<ChannelAddress, JsonElement> channels = dataEntry.getValue();

			// Check if cache is valid (it is not elder than 5 minutes compared to this
			// timestamp)
			long cacheTimestamp = edgeCache.getTimestamp();
			if (timestamp < cacheTimestamp) {
				// incoming data is older than cache -> do not apply cache
			} else {
				// incoming data is more recent than cache
				// update cache timestamp
				edgeCache.setTimestamp(timestamp);

				if (timestamp < cacheTimestamp + 5 * 60 * 1000) {
					// cache is valid (not elder than 5 minutes)
				} else {
					// cache is not anymore valid (elder than 5 minutes)
					// clear cache
					if (cacheTimestamp != 0L) {
						this.logInfo(this.log, "Edge [" + edgeId + "]: invalidate cache. This timestamp [" + timestamp
								+ "]. Cache timestamp [" + cacheTimestamp + "]");
					}
					edgeCache.clear();
				}

				// add incoming data to cache (this replaces already existing cache values)
				for (Entry<ChannelAddress, JsonElement> channelEntry : channels.entrySet()) {
					ChannelAddress channel = channelEntry.getKey();
					JsonElement value = channelEntry.getValue();
					edgeCache.putToChannelCache(channel, value);
				}
			}
		}
	}

	@Override
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, int resolution)
			throws OpenemsNamedException {
		this.logWarn(this.log, "I do not support querying historic data");
		return new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();
	}

	@Override
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		this.logWarn(this.log, "I do not support querying historic energy");
		return null;
	}

}
