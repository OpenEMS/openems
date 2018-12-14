package io.openems.backend.timedata.influx;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBIOException;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.timedata.api.Timedata;
import io.openems.backend.timedata.core.EdgeCache;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Tag;
import io.openems.common.timedata.TimedataUtils;
import io.openems.common.types.ChannelAddress;
import io.openems.shared.influxdb.InfluxConnector;

@Designate(ocd = Config.class, factory = false)
@Component(name = "Timedata.InfluxDB", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Influx implements Timedata {

	private final static String TMP_MINI_MEASUREMENT = "minies";
	private final static String TAG = "fems";

	private final Logger log = LoggerFactory.getLogger(Influx.class);
	private final Map<String, EdgeCache> edgeCacheMap = new HashMap<>();

	private InfluxConnector influxConnector = null;

	@Reference
	protected volatile Metadata metadata;

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

	@Override
	public void write(String edgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) throws OpenemsException {
		// parse the numeric EdgeId
		int influxEdgeId = TimedataUtils.parseNumberFromName(edgeId);

		// get existing or create new DeviceCache
		EdgeCache edgeCache = this.edgeCacheMap.get(edgeId);
		if (edgeCache == null) {
			edgeCache = new EdgeCache();
			this.edgeCacheMap.put(edgeId, edgeCache);
		}

		/*
		 * Prepare data table. Takes entries starting with eldest timestamp (ascending
		 * order)
		 */
		for (Entry<Long, Map<ChannelAddress, JsonElement>> entry : data.rowMap().entrySet()) {
			Long timestamp = entry.getKey();

			// Check if cache is valid (it is not elder than 5 minutes compared to this
			// timestamp)
			long cacheTimestamp = edgeCache.getTimestamp();
			if (timestamp < cacheTimestamp) {
				// incoming data is older than cache -> do not apply cache

			} else {
				// incoming data is more recent than cache
				if (timestamp < cacheTimestamp + 5 * 60 * 1000) {
					// cache is valid (not elder than 5 minutes)
					for (Entry<ChannelAddress, JsonElement> cacheEntry : edgeCache.getChannelCacheEntries()) {
						ChannelAddress channel = cacheEntry.getKey();
						// check if there is a current value for this timestamp + channel
						JsonElement existingValue = data.get(timestamp, channel);
						if (existingValue == null) {
							// if not -> add cache data to write data
							data.put(timestamp, channel, cacheEntry.getValue());
						}
					}
				} else {
					// cache is not anymore valid (elder than 5 minutes)
					if (cacheTimestamp != 0l) {
						log.info("Edge [" + edgeId + "]: invalidate cache for influxId [" + influxEdgeId
								+ "]. This timestamp [" + timestamp + "]. Cache timestamp [" + cacheTimestamp + "]");
					}
					// clear cache
					edgeCache.clear();
				}

				// update cache
				edgeCache.setTimestamp(timestamp);
				for (Entry<ChannelAddress, JsonElement> channelEntry : entry.getValue().entrySet()) {
					edgeCache.putToChannelCache(channelEntry.getKey(), channelEntry.getValue());
				}
			}
		}

		// Write data to default location
		this.writeData(influxEdgeId, data);

		// Hook to continue writing data to old Mini monitoring
		this.writeDataToOldMiniMonitoring(edgeId, influxEdgeId, data);
	}

	private void writeData(int influxEdgeId, TreeBasedTable<Long, ChannelAddress, JsonElement> data) {
		InfluxDB influxDB = this.influxConnector.getConnection();

		BatchPoints batchPoints = BatchPoints.database(this.influxConnector.getDatabase()) //
				.tag(Influx.TAG, String.valueOf(influxEdgeId)) //
				.build();

		for (Entry<Long, Map<ChannelAddress, JsonElement>> entry : data.rowMap().entrySet()) {
			Long timestamp = entry.getKey();
			// this builds an InfluxDB record ("point") for a given timestamp
			Builder builder = Point.measurement(InfluxConnector.MEASUREMENT).time(timestamp, TimeUnit.MILLISECONDS);
			for (Entry<ChannelAddress, JsonElement> channelEntry : entry.getValue().entrySet()) {
				Influx.addValue(builder, channelEntry.getKey().toString(), channelEntry.getValue());
			}
			batchPoints.point(builder.build());
		}

		// write to DB
		try {
			influxDB.write(batchPoints);
		} catch (InfluxDBIOException e) {
			this.log.error("Unable to write data: " + e.getMessage());
		}
	}

	/**
	 * Adds the value in the correct data format for InfluxDB
	 *
	 * @param channel
	 * @param jValueElement
	 * @return
	 */
	private static void addValue(Builder builder, String field, JsonElement element) {
		if (element == null || element.isJsonNull()) {
			// do not add
			return;
		}
		if (element.isJsonPrimitive()) {
			JsonPrimitive value = element.getAsJsonPrimitive();
			if (value.isNumber()) {
				try {
					builder.addField(field, Long.parseLong(value.toString()));
				} catch (NumberFormatException e1) {
					try {
						builder.addField(field, Double.parseDouble(value.toString()));
					} catch (NumberFormatException e2) {
						builder.addField(field, value.getAsNumber());
					}
				}
			} else if (value.isBoolean()) {
				builder.addField(field, value.getAsBoolean());
			} else if (value.isString()) {
				builder.addField(field, value.getAsString());
			} else {
				builder.addField(field, value.toString());
			}
		} else {
			builder.addField(field, element.toString());
		}
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
	private void writeDataToOldMiniMonitoring(String edgeId, int influxId,
			TreeBasedTable<Long, ChannelAddress, JsonElement> data) throws OpenemsException {
		Edge edge = this.metadata.getEdgeOrError(edgeId);
		if (!edge.getProducttype().equals("MiniES 3-3")) {
			return;
		}

		InfluxDB influxDB = this.influxConnector.getConnection();

		BatchPoints batchPoints = BatchPoints.database(this.influxConnector.getDatabase()) //
				.tag(Influx.TAG, String.valueOf(influxId)) //
				.build();

		for (Entry<Long, Map<ChannelAddress, JsonElement>> entry : data.rowMap().entrySet()) {
			Long timestamp = entry.getKey();
			Builder builder = Point.measurement(TMP_MINI_MEASUREMENT).time(timestamp, TimeUnit.MILLISECONDS);

			Map<String, Object> fields = new HashMap<>();

			for (Entry<ChannelAddress, JsonElement> valueEntry : entry.getValue().entrySet()) {
				String channel = valueEntry.getKey().toString();
				JsonElement element = valueEntry.getValue();
				if (element.isJsonPrimitive()) {
					long value = element.getAsJsonPrimitive().getAsLong();

					// convert channel ids to old identifiers
					if (channel.equals("ess0/Soc")) {
						fields.put("Stack_SOC", value);
						edge.setSoc((int) value);
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

	public Optional<JsonElement> getChannelValue(String edgeId, ChannelAddress address) {
		EdgeCache cache = this.edgeCacheMap.get(edgeId);
		if (cache != null) {
			Optional<Edge> edgeOpt = this.metadata.getEdge(edgeId);
			if (!edgeOpt.isPresent()) {
				return cache.getChannelValue(address);
			}
			Edge edge = edgeOpt.get();
			ChannelFormula[] compatibility = getCompatibilityFormula(edge, address);
			if (compatibility.length == 0) {
				return cache.getChannelValue(address);
			}
			// handle compatibility with elder OpenEMS Edge version
			return this.getCompatibilityChannelValue(compatibility, cache);
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Handles compatibility with elder OpenEMS Edge version, e.g. calculate the
	 * '_sum' Channels.
	 * 
	 * @param compatibility
	 * @param cache
	 * @return
	 */
	private Optional<JsonElement> getCompatibilityChannelValue(ChannelFormula[] compatibility, EdgeCache cache) {
		int value = 0;
		for (ChannelFormula formula : compatibility) {
			ChannelAddress addr = formula.getAddress();
			switch (formula.getFunction()) {
			case PLUS:
				value += cache.getChannelValue(addr).orElse(new JsonPrimitive(0)).getAsInt();
			}
		}
		return Optional.of(new JsonPrimitive(value));
	}

	enum Function {
		PLUS
	}

	class ChannelFormula {
		private final Function function;
		private final ChannelAddress address;

		public ChannelFormula(Function function, ChannelAddress address) {
			this.function = function;
			this.address = address;
		}

		public Function getFunction() {
			return function;
		}

		public ChannelAddress getAddress() {
			return address;
		}
	}

	private ChannelFormula[] getCompatibilityFormula(Edge edge, ChannelAddress address) {
		if (address.getComponentId().equals("_sum")) {
			JsonObject config = edge.getConfig();
			switch (address.getChannelId()) {
			case "EssSoc":
				return new ChannelFormula[] { //
						new ChannelFormula(Function.PLUS, new ChannelAddress("ess0", "Soc")) };
			case "EssActivePower":
				return new ChannelFormula[] { //
						new ChannelFormula(Function.PLUS, new ChannelAddress("ess0", "ActivePower")) };
			}
		}
		return new ChannelFormula[0];
	}

	@Override
	public JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution, Tag... tags) throws OpenemsException {
		return this.influxConnector.queryHistoricData(fromDate, toDate, channels, resolution, tags);
	}
}
