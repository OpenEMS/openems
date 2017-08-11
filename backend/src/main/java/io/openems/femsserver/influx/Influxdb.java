package io.openems.femsserver.influx;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.femsserver.utilities.JsonUtils;
import io.openems.femsserver.utilities.OpenemsException;

public class Influxdb {

	protected String database;
	protected String url;
	protected int port;
	protected String username;
	protected String password;

	private final String DB_NAME = "db";

	private static Logger log = LoggerFactory.getLogger(Influxdb.class);

	private static Influxdb instance;

	private static HashMap<String, Object> hmapData = new HashMap<String, Object>();
	private static Long lastTimestamp = Long.valueOf(0);

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
	}

	/**
	 * Takes a JsonObject and writes the points to influxDB.
	 *
	 * Format: { "timestamp1" { "channel1": value, "channel2": value },
	 * "timestamp2" { "channel1": value, "channel2": value } }
	 */
	public void write(String fems, JsonObject jData) {
		BatchPoints batchPoints = BatchPoints.database(database) //
				.tag("fems", fems) //
				.retentionPolicy("default").build();

		TreeMap<Long, JsonObject> jDataTree = new TreeMap<Long, JsonObject>();

		jData.entrySet().forEach(timestampEntry -> {
			String timestampString = timestampEntry.getKey();
			Long timestamp = Long.valueOf(timestampString);
			JsonObject jChannels;
			try {
				jChannels = JsonUtils.getAsJsonObject(timestampEntry.getValue());
				jDataTree.put(timestamp, jChannels);
			} catch (OpenemsException e) {
				log.error("InfluxDB data error: " + e.getMessage());
			}
		});

		jDataTree.entrySet().forEach(timestampEntry -> {
			Long timestamp = timestampEntry.getKey();
			if (timestamp > lastTimestamp) {
				JsonObject jChannels;
				try {
					jChannels = JsonUtils.getAsJsonObject(timestampEntry.getValue());
					if (jChannels.entrySet().size() > 0) {
						Builder builder = Point.measurement("data") //
								.time(timestamp, TimeUnit.MILLISECONDS);
						jChannels.entrySet().forEach(jChannelEntry -> {
							try {
								String channel = jChannelEntry.getKey();
								JsonPrimitive jValue = JsonUtils.getAsPrimitive(jChannelEntry.getValue());
								if (jValue.isNumber()) {
									hmapData.put(channel, jValue.getAsNumber());
								} else if (jValue.isString()) {
									hmapData.put(channel, jValue.getAsString());
								} else {
									log.warn(fems + ": Ignore unknown type [" + jValue + "] for channel [" + channel
											+ "]");
								}
							} catch (OpenemsException e) {
								log.error("InfluxDB data error: " + e.getMessage());
							}
						});
						for (String key : hmapData.keySet()) {
							if (hmapData.get(key) instanceof Number) {
								builder.addField(key, (Number) hmapData.get(key));
							} else if (hmapData.get(key) instanceof String) {
								builder.addField(key, (String) hmapData.get(key));
							}
						}
						batchPoints.point(builder.build());
						lastTimestamp = timestamp;
					}
				} catch (OpenemsException e) {
					log.error("InfluxDB data error: " + e.getMessage());
				}
			} else {
				// current timestamp is smaller than the last one saved in the DB
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
