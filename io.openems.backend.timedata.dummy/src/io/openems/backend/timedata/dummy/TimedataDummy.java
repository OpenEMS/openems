package io.openems.backend.timedata.dummy;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.timedata.api.Timedata;
import io.openems.backend.timedata.core.EdgeCache;
import io.openems.backend.timedata.core.Utils;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Timedata.Dummy", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TimedataDummy implements Timedata {

	private final Logger log = LoggerFactory.getLogger(TimedataDummy.class);
	private final Map<String, EdgeCache> edgeCacheMap = new HashMap<>();

	@Reference
	protected volatile Metadata metadata;

	@Activate
	void activate(Config config) throws OpenemsException {
		log.info("Activate Timedata.Dummy");
	}

	@Deactivate
	void deactivate() {
		log.info("Deactivate Timedata.Dummy");
	}

	public Optional<Object> getChannelValue(String edgeId, ChannelAddress channelAddress) {
		EdgeCache edgeCache = this.edgeCacheMap.get(edgeId);
		if (edgeCache != null) {
			return edgeCache.getChannelValueOpt(channelAddress.toString());
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Takes a JsonObject and writes the points to influxDB.
	 *
	 * Format:
	 * 
	 * <pre>
	 * {
	 *   "timestamp1" {
	 * 	   "channel1": value,
	 *     "channel2": value 
	 *   }, "timestamp2" {
	 *     "channel1": value,
	 *     "channel2": value
	 *   }
	 * }
	 * </pre>
	 */
	public void write(String edgeId, JsonObject jData) throws OpenemsException {
		Edge edge = this.metadata.getEdgeOrError(edgeId);

		// get existing or create new EdgeCache
		EdgeCache edgeCache = this.edgeCacheMap.get(edgeId);
		if (edgeCache == null) {
			edgeCache = new EdgeCache();
			this.edgeCacheMap.put(edgeId, edgeCache);
		}

		// Sort incoming data by timestamp
		TreeMap<Long, JsonObject> sortedData = new TreeMap<Long, JsonObject>();
		for (Entry<String, JsonElement> entry : jData.entrySet()) {
			try {
				Long timestamp = Long.valueOf(entry.getKey());
				JsonObject jChannels;
				jChannels = JsonUtils.getAsJsonObject(entry.getValue());
				sortedData.put(timestamp, jChannels);
			} catch (OpenemsException e) {
				log.error("Data error: " + e.getMessage());
			}
		}

		// Prepare data table. Takes entries starting with eldest timestamp (ascending
		// order)
		for (Entry<Long, JsonObject> dataEntry : sortedData.entrySet()) {
			Long timestamp = dataEntry.getKey();
			JsonObject jChannels = dataEntry.getValue();

			if (jChannels.entrySet().size() == 0) {
				// no channel values available. abort.
				continue;
			}

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
					if (cacheTimestamp != 0l) {
						log.info("Edge [" + edge.getId() + "]: invalidate cache for edge [" + edgeId
								+ "]. This timestamp [" + timestamp + "]. Cache timestamp [" + cacheTimestamp + "]");
					}
					edgeCache.clear();
				}

				// add incoming data to cache (this replaces already existing cache values)
				for (Entry<String, JsonElement> channelEntry : jChannels.entrySet()) {
					String channel = channelEntry.getKey();
					Optional<Object> valueOpt = Utils.parseValue(channel, channelEntry.getValue());
					if (valueOpt.isPresent()) {
						Object value = valueOpt.get();
						edgeCache.putToChannelCache(channel, value);
					}
				}
			}
		}
	}

	@Override
	public JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution, io.openems.common.timedata.Tag... tags) throws OpenemsException {
		this.log.error("Timedata.Dummy does not support querying historic data");
		return new JsonArray();
	}
}
