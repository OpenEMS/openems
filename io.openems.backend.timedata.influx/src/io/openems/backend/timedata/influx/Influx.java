package io.openems.backend.timedata.influx;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.MetadataService;
import io.openems.backend.timedata.api.TimedataService;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Tag;
import io.openems.common.timedata.TimedataUtils;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.shared.influxdb.InfluxConnector;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Timedata.InfluxDB", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Influx implements TimedataService {

	private final Logger log = LoggerFactory.getLogger(Influx.class);

	private InfluxConnector influxConnector = null;

	private final String TMP_MINI_MEASUREMENT = "minies";

	@Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
	protected volatile MetadataService metadataService;

	private final Map<Integer, DeviceCache> deviceCacheMap = new HashMap<>();

	@Activate
	void activate(Config config) throws OpenemsException {
		log.info("Activate Timedata.InfluxDB [url=" + config.url() + ";port=" + config.port() + ";database="
				+ config.database() + ";username=" + config.username() + ";password="
				+ (config.password() != null ? "ok" : "NOT_SET") + ";measurement=" + config.measurement() + "]");

		this.influxConnector = new InfluxConnector(config.url(), config.port(), config.username(), config.password(),
				config.database());
	}

	@Deactivate
	void deactivate() {
		log.info("Deactivate Timedata.InfluxDB");
		if (this.influxConnector != null) {
			this.influxConnector.deactivate();
		}
	}

	private void writeData(int influxId, TreeBasedTable<Long, String, Object> data) throws OpenemsException {
		InfluxDB influxDB = this.influxConnector.getConnection();

		BatchPoints batchPoints = BatchPoints.database(this.influxConnector.getDatabase()) //
				.tag("fems", String.valueOf(influxId)) //
				.build();

		for (Entry<Long, Map<String, Object>> entry : data.rowMap().entrySet()) {
			Long timestamp = entry.getKey();
			// this builds an InfluxDB record ("point") for a given timestamp
			Builder builder = Point.measurement(InfluxConnector.MEASUREMENT).time(timestamp, TimeUnit.MILLISECONDS)
					.fields(entry.getValue());
			batchPoints.point(builder.build());
		}

		// write to DB
		influxDB.write(batchPoints);
	}

	/**
	 * Writes data to old database for old Mini monitoring
	 * 
	 * XXX remove after full migration
	 *
	 * @param device
	 * @param data
	 * @throws OpenemsException
	 */
	private void writeDataToOldMiniMonitoring(Edge edge, int influxId, TreeBasedTable<Long, String, Object> data)
			throws OpenemsException {
		InfluxDB influxDB = this.influxConnector.getConnection();

		BatchPoints batchPoints = BatchPoints.database(this.influxConnector.getDatabase()) //
				.tag("fems", String.valueOf(influxId)) //
				.build();

		for (Entry<Long, Map<String, Object>> entry : data.rowMap().entrySet()) {
			Long timestamp = entry.getKey();
			Builder builder = Point.measurement(TMP_MINI_MEASUREMENT).time(timestamp, TimeUnit.MILLISECONDS);

			Map<String, Object> fields = new HashMap<>();

			for (Entry<String, Object> valueEntry : entry.getValue().entrySet()) {
				String channel = valueEntry.getKey();
				Object valueObj = valueEntry.getValue();
				if (valueObj instanceof Number) {
					Long value = ((Number) valueObj).longValue();

					// convert channel ids to old identifiers
					if (channel.equals("ess0/Soc")) {
						fields.put("Stack_SOC", value);
						edge.setSoc(value.intValue());
					} else if (channel.equals("meter0/ActivePower")) {
						fields.put("PCS_Grid_Power_Total", value * -1);
					} else if (channel.equals("meter1/ActivePower")) {
						fields.put("PCS_PV_Power_Total", value);
					} else if (channel.equals("meter2/ActivePower")) {
						fields.put("PCS_Load_Power_Total", value);
					}

					// from here value needs to be divided by 10 for backwards compatibility
					value = value / 10;
					if (channel.equals("meter2/Energy")) {
						fields.put("PCS_Summary_Consumption_Accumulative_cor", value);
						fields.put("PCS_Summary_Consumption_Accumulative", value);
					} else if (channel.equals("meter0/BuyFromGridEnergy")) {
						fields.put("PCS_Summary_Grid_Buy_Accumulative_cor", value);
						fields.put("PCS_Summary_Grid_Buy_Accumulative", value);
					} else if (channel.equals("meter0/SellToGridEnergy")) {
						fields.put("PCS_Summary_Grid_Sell_Accumulative_cor", value);
						fields.put("PCS_Summary_Grid_Sell_Accumulative", value);
					} else if (channel.equals("meter1/EnergyL1")) {
						fields.put("PCS_Summary_PV_Accumulative_cor", value);
						fields.put("PCS_Summary_PV_Accumulative", value);
					}
				}
			}

			if (fields.size() > 0) {
				builder.fields(fields);
				batchPoints.point(builder.build());
			}
		}

		// write to DB
		influxDB.write(batchPoints);
	}

	public Optional<Object> getChannelValue(int edgeId, ChannelAddress channelAddress) {
		DeviceCache deviceCache = this.deviceCacheMap.get(edgeId);
		if (deviceCache != null) {
			return deviceCache.getChannelValueOpt(channelAddress.toString());
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
	public void write(int edgeId, JsonObject jData) throws OpenemsException {
		Edge edge = this.metadataService.getEdge(edgeId);
		int influxId = TimedataUtils.parseNumberFromName(edge.getName());
		TreeBasedTable<Long, String, Object> data = TreeBasedTable.create();

		// get existing or create new DeviceCache
		DeviceCache deviceCache = this.deviceCacheMap.get(edgeId);
		if (deviceCache == null) {
			deviceCache = new DeviceCache();
			this.deviceCacheMap.put(edgeId, deviceCache);
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
			long cacheTimestamp = deviceCache.getTimestamp();
			if (timestamp < cacheTimestamp) {
				// incoming data is older than cache -> do not apply cache
			} else {
				// incoming data is more recent than cache
				// update cache timestamp
				deviceCache.setTimestamp(timestamp);

				if (timestamp < cacheTimestamp + 5 * 60 * 1000) {
					// cache is valid (not elder than 5 minutes)
					// add cache data to write data
					for (Entry<String, Object> cacheEntry : deviceCache.getChannelCacheEntries()) {
						String channel = cacheEntry.getKey();
						Object value = cacheEntry.getValue();
						data.put(timestamp, channel, value);
					}
				} else {
					// cache is not anymore valid (elder than 5 minutes)
					// clear cache
					if (cacheTimestamp != 0l) {
						log.info("Edge [" + edge.getName() + "]: invalidate cache for influxId [" + influxId
								+ "]. This timestamp [" + timestamp + "]. Cache timestamp [" + cacheTimestamp + "]");
					}
					deviceCache.clear();
				}

				// add incoming data to cache (this replaces already existing cache values)
				for (Entry<String, JsonElement> channelEntry : jChannels.entrySet()) {
					String channel = channelEntry.getKey();
					Optional<Object> valueOpt = Utils.parseValue(channel, channelEntry.getValue());
					if (valueOpt.isPresent()) {
						Object value = valueOpt.get();
						deviceCache.putToChannelCache(channel, value);
					}
				}
			}

			// add incoming data to write data
			for (Entry<String, JsonElement> channelEntry : jChannels.entrySet()) {
				String channel = channelEntry.getKey();
				Optional<Object> valueOpt = Utils.parseValue(channel, channelEntry.getValue());
				if (valueOpt.isPresent()) {
					Object value = valueOpt.get();
					data.put(timestamp, channel, value);
				}
			}
		}

		// Write data to default location
		writeData(influxId, data);

		// Hook to continue writing data to old Mini monitoring
		if (edge.getProducttype().equals("MiniES 3-3")) {
			writeDataToOldMiniMonitoring(edge, influxId, data);
		}
	}

	@Override
	public JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution, boolean cumulative, Tag... tags) throws OpenemsException {
		return this.influxConnector.queryHistoricData(fromDate, toDate, channels, resolution, cumulative, tags);
	}

	@Override
	public JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution, Tag... tags) throws OpenemsException {
		return this.influxConnector.queryHistoricData(fromDate, toDate, channels, resolution, false, tags);

	}
}
