package io.openems.backend.influx;

import java.time.ZonedDateTime;
import java.util.Map;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.backend.utilities.JsonUtils;
import io.openems.backend.utilities.OpenemsException;

public class Influxdb {

	protected String database;
	protected String url;
	protected int port;
	protected String username;
	protected String password;

	private final String DB_NAME = "db";

	private static Logger log = LoggerFactory.getLogger(Influxdb.class);
	private static Influxdb instance;

	public static void initialize(String database, String url, int port, String username, String password)
			throws Exception {
		if (database == null || url == null || username == null || password == null) {
			throw new Exception("Config missing: database [" + database + "], url [" + url + "], port [" + port
					+ "] username [" + username + "], password [" + password + "]");
		}
		Influxdb influxdb = getInstance();
		influxdb.database = database;
		influxdb.url = url;
		influxdb.port = port;
		influxdb.username = username;
		influxdb.password = password;
		influxdb.connect();
	}

	public static synchronized Influxdb getInstance() throws Exception {
		if (Influxdb.instance == null) {
			Influxdb.instance = new Influxdb();
		}
		return Influxdb.instance;
	}

	private InfluxDB influxDB;
	// 1st: fems; 2nd: channel; 3rd: value
	private Table<String, String, Object> lastDataCache = Tables.synchronizedTable(HashBasedTable.create());
	// fems : timestamp
	private Map<String, Long> lastTimestampMap = new ConcurrentHashMap<String, Long>();

	private Influxdb() {

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
	public void write(String fems, JsonObject jData) {
		final Long lastTimestamp = this.lastTimestampMap.getOrDefault(fems, 0l);

		BatchPoints batchPoints = BatchPoints.database(database) //
				.tag("fems", fems) //
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
		data.entrySet().forEach(dataEntry -> {
			Long timestamp = dataEntry.getKey();
			// use lastDataCache only if we receive the latest data and cache is not elder than 1 minute
			boolean useLastDataCache = timestamp > lastTimestamp && timestamp < lastTimestamp + 60000;
			this.lastTimestampMap.put(fems, timestamp);
			Builder builder = Point.measurement("data") // this builds a InfluxDB record ("point") for a given timestamp
					.time(timestamp, TimeUnit.MILLISECONDS);

			JsonObject jChannels = dataEntry.getValue();
			if (jChannels.entrySet().size() > 0) {
				jChannels.entrySet().forEach(channelEntry -> {
					String channel = channelEntry.getKey();
					JsonPrimitive jValue;
					try {
						jValue = JsonUtils.getAsPrimitive(channelEntry.getValue());
						if (jValue.isNumber()) {
							Number value = jValue.getAsNumber();
							builder.addField(channel, value);
							if (useLastDataCache) {
								this.lastDataCache.put(fems, channel, value);
							}

						} else if (jValue.isString()) {
							String value = jValue.getAsString();
							builder.addField(channel, value);
							if (useLastDataCache) {
								this.lastDataCache.put(fems, channel, value);
							}

						} else {
							log.warn(fems + ": Ignore unknown type [" + jValue + "] for channel [" + channel + "]");
						}
					} catch (OpenemsException e) {
						log.error("Data error: " + e.getMessage());
					}

				});

				// only for latest data: add the cached data to the InfluxDB point.
				if (useLastDataCache) {
					this.lastDataCache.row(fems).entrySet().forEach(cacheEntry -> {
						String field = cacheEntry.getKey();
						Object value = cacheEntry.getValue();
						if (value instanceof Number) {
							builder.addField(field, (Number) value);
						} else if (value instanceof String) {
							builder.addField(field, (String) value);
						} else {
							log.warn("Unknown type in InfluxDB. This should never happen.");
						}
					});
				}

				// add the point to the batch
				batchPoints.point(builder.build());
			}
		});
		// write to DB
		influxDB.write(batchPoints);

	}

	public JsonObject query(int _fems, ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution/* , JsonObject kWh */) throws OpenemsException {
		Optional<Integer> fems = Optional.of(_fems);
		Optional<InfluxDB> influxdb = Optional.of(influxDB);
		return InfluxdbQueryWrapper.query(influxdb, fems, fromDate, toDate, channels, resolution/* , kWh */);
	}
}
