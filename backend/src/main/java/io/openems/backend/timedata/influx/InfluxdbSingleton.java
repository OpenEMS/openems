package io.openems.backend.timedata.influx;

import java.text.NumberFormat;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.backend.timedata.api.TimedataSingleton;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.InfluxdbUtils;
import io.openems.common.utils.JsonUtils;

public class InfluxdbSingleton implements TimedataSingleton {

	private final Logger log = LoggerFactory.getLogger(InfluxdbSingleton.class);

	private String database;
	private String url;
	private int port;
	private String username;
	private String password;

	private InfluxDB influxDB;
	// 1st: deviceId; 2nd: channel; 3rd: value
	private Table<Integer, String, Object> lastDataCache = Tables.synchronizedTable(HashBasedTable.create());
	// key: deviceId; value: timestamp
	private Map<Integer, Long> lastTimestampMap = new ConcurrentHashMap<Integer, Long>();

	public InfluxdbSingleton(String database, String url, int port, String username, String password) throws Exception {
		this.database = database;
		this.url = url;
		this.port = port;
		this.username = username;
		this.password = password;
		this.connect();
	}

	private void connect() throws Exception {
		InfluxDB influxDB = InfluxDBFactory.connect("http://" + url + ":" + port, username, password);
		this.influxDB = influxDB;
		try {
			influxDB.ping();
		} catch (RuntimeException e) {
			log.error("Unable to connect to InfluxDB: " + e.getMessage());
			throw new Exception(e.getMessage());
		}
		/*
		 * try {
		 * influxDB.createDatabase(DB_NAME);
		 * } catch (RuntimeException e) {
		 * log.error("Unable to create InfluxDB database: " + DB_NAME);
		 * throw new Exception(e.getMessage());
		 * }
		 */
	}

	/**
	 * Takes a JsonObject and writes the points to influxDB.
	 *
	 * Format: { "timestamp1" { "channel1": value, "channel2": value },
	 * "timestamp2" { "channel1": value, "channel2": value } }
	 */
	@Override
	public void write(Optional<Integer> deviceIdOpt, JsonObject jData) {
		int deviceId = deviceIdOpt.orElse(0);
		long lastTimestamp = this.lastTimestampMap.getOrDefault(deviceId, 0l);

		BatchPoints batchPoints = BatchPoints.database(database) //
				.tag("fems", String.valueOf(deviceId)) //
				.build();

		// Sort data by timestamp
		TreeMap<Long, JsonObject> data = new TreeMap<Long, JsonObject>();
		jData.entrySet().forEach(timestampEntry -> {
			String timestampString = timestampEntry.getKey();
			Long timestamp = Long.valueOf(timestampString);
			JsonObject jChannels;
			try {
				jChannels = JsonUtils.getAsJsonObject(timestampEntry.getValue());
				data.put(timestamp, jChannels);
			} catch (OpenemsException e) {
				log.error("Data error: " + e.getMessage());
			}
		});

		// Prepare data for writing to InfluxDB
		for (Entry<Long, JsonObject> dataEntry : data.entrySet()) {
			Long timestamp = dataEntry.getKey();
			// use lastDataCache only if we receive the latest data and cache is not elder than 1 minute
			boolean useLastDataCache = timestamp > lastTimestamp && timestamp < lastTimestamp + 60000;
			this.lastTimestampMap.put(deviceId, timestamp);
			Builder builder = Point.measurement("data") // this builds an InfluxDB record ("point") for a given
														// timestamp
					.time(timestamp, TimeUnit.MILLISECONDS);

			JsonObject jChannels = dataEntry.getValue();
			if (jChannels.entrySet().size() > 0) {
				jChannels.entrySet().forEach(channelEntry -> {
					String channel = channelEntry.getKey();
					Optional<Object> valueOpt = this.addChannelToBuilder(builder, channel, channelEntry.getValue());
					if (valueOpt.isPresent() && useLastDataCache) {
						this.lastDataCache.put(deviceId, channel, valueOpt.get());
					}

				});

				// only for latest data: add the cached data to the InfluxDB point.
				if (useLastDataCache) {
					this.lastDataCache.row(deviceId).entrySet().forEach(cacheEntry -> {
						String channel = cacheEntry.getKey();
						Object value = cacheEntry.getValue();
						this.addChannelToBuilder(builder, channel, value);
					});
				}

				// add the point to the batch
				batchPoints.point(builder.build());

				// set last timestamp
				lastTimestamp = timestamp;
			}
		}
		// write to DB
		influxDB.write(batchPoints);
	}

	/**
	 * Add value to Influx Builder in the correct data format
	 *
	 * @param builder
	 * @param channel
	 * @param value
	 * @return
	 */
	private Optional<Object> addChannelToBuilder(Builder builder, String channel, Object value) {
		if (value == null) {
			return Optional.empty();
		}
		// convert JsonElement
		if (value instanceof JsonElement) {
			JsonElement jValueElement = (JsonElement) value;
			if (jValueElement.isJsonPrimitive()) {
				JsonPrimitive jValue = jValueElement.getAsJsonPrimitive();
				if (jValue.isNumber()) {
					try {
						// Avoid GSONs LazilyParsedNumber
						value = NumberFormat.getInstance().parse(jValue.toString());
					} catch (ParseException e) {
						log.error("Unable to parse Number: " + e.getMessage());
						value = jValue.getAsNumber();
					}
				} else if (jValue.isBoolean()) {
					value = jValue.getAsBoolean();
				} else if (jValue.isString()) {
					value = jValue.getAsString();
				}
			}
		}
		if (value instanceof Number) {
			Number numberValue = (Number) value;
			if (numberValue instanceof Integer) {
				builder.addField(channel, numberValue.intValue());
				return Optional.of(numberValue.intValue());
			} else if (numberValue instanceof Double) {
				builder.addField(channel, numberValue.doubleValue());
				return Optional.of(numberValue.doubleValue());
			} else {
				builder.addField(channel, numberValue);
				return Optional.of(numberValue);
			}
		} else if (value instanceof Boolean) {
			builder.addField(channel, (Boolean) value);
			return Optional.of(value);
		} else if (value instanceof String) {
			builder.addField(channel, (String) value);
			return Optional.of(value);
		}
		log.warn("Unknown type of value [" + value + "] channel [" + channel + "]. This should never happen.");
		return Optional.empty();
	}

	@Override
	public JsonArray queryHistoricData(Optional<Integer> deviceIdOpt, ZonedDateTime fromDate, ZonedDateTime toDate,
			JsonObject channels, int resolution) throws OpenemsException {
		return InfluxdbUtils.queryHistoricData(influxDB, this.database, deviceIdOpt, fromDate, toDate, channels,
				resolution);
	}
}
