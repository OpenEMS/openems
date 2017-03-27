package io.openems.femsserver.influx;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.femsserver.utilities.Address;
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

		jData.entrySet().forEach(timestampEntry -> {
			String timestampString = timestampEntry.getKey();
			JsonObject jChannels;
			try {
				jChannels = JsonUtils.getAsJsonObject(timestampEntry.getValue());
				if (jChannels.entrySet().size() > 0) {
					Long timestamp = Long.valueOf(timestampString);
					Builder builder = Point.measurement("data") //
							.time(timestamp, TimeUnit.MILLISECONDS);
					jChannels.entrySet().forEach(jChannelEntry -> {
						try {
							String channel = jChannelEntry.getKey();
							JsonPrimitive jValue = JsonUtils.getAsPrimitive(jChannelEntry.getValue());
							if (jValue.isNumber()) {
								builder.addField(channel, jValue.getAsNumber());
							} else if (jValue.isString()) {
								builder.addField(channel, jValue.getAsString());
							} else {
								log.warn(fems + ": Ignore unknown type [" + jValue + "] for channel [" + channel + "]");
							}
						} catch (OpenemsException e) {
							log.error("InfluxDB data error: " + e.getMessage());
						}
					});
					batchPoints.point(builder.build());
				}
			} catch (OpenemsException e) {
				log.error("InfluxDB data error: " + e.getMessage());
			}
		});
		// write to DB
		influxDB.write(batchPoints);
	}

	public JsonArray query(int fems, ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels, int resolution)
			throws OpenemsException {
		// Prepare query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(toChannelAddressList(channels));
		query.append(" FROM data WHERE fems = '");
		query.append(fems);
		query.append("' AND time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.plusDays(1).toEpochSecond()));
		query.append("s");
		query.append(" GROUP BY time(");
		query.append(resolution);
		query.append("s)");

		// Parse result
		QueryResult queryResult = influxDB.query(new Query(query.toString(), DB_NAME), TimeUnit.MILLISECONDS);
		if (queryResult.hasError()) {
			throw new OpenemsException("InfluxDB query error: " + queryResult.getError());
		}

		JsonArray j = new JsonArray();
		for (Result result : queryResult.getResults()) {
			List<Series> seriess = result.getSeries();
			if (seriess != null) {
				for (Series series : seriess) {
					// create thing/channel index
					ArrayList<Address> addressIndex = new ArrayList<>();
					for (String column : series.getColumns()) {
						if (column.equals("time")) {
							continue;
						}
						addressIndex.add(Address.fromString(column));
					}
					// first: create empty timestamp objects
					for (List<Object> values : series.getValues()) {
						JsonObject jTimestamp = new JsonObject();
						// get timestamp
						Instant timestampInstant = Instant.ofEpochMilli((long) ((Double) values.get(0)).doubleValue());
						ZonedDateTime timestamp = ZonedDateTime.ofInstant(timestampInstant, fromDate.getZone());
						String timestampString = timestamp.format(DateTimeFormatter.ISO_INSTANT);
						jTimestamp.addProperty("time", timestampString);
						// add empty channels by copying "channels" parameter
						JsonObject jChannels = new JsonObject();
						for (Entry<String, JsonElement> entry : channels.entrySet()) {
							String thingId = entry.getKey();
							JsonObject jThing = new JsonObject();
							JsonArray channelIds = JsonUtils.getAsJsonArray(entry.getValue());
							for (JsonElement channelElement : channelIds) {
								String channelId = JsonUtils.getAsString(channelElement);
								jThing.add(channelId, JsonNull.INSTANCE);
							}
							jChannels.add(thingId, jThing);
						}
						jTimestamp.add("channels", jChannels);
						j.add(jTimestamp);
					}
					// then: add all data
					for (int columnIndex = 1; columnIndex < series.getColumns().size(); columnIndex++) {
						for (int timeIndex = 0; timeIndex < series.getValues().size(); timeIndex++) {
							Double value = (Double) series.getValues().get(timeIndex).get(columnIndex);
							Address address = addressIndex.get(columnIndex - 1);
							j.get(timeIndex).getAsJsonObject().get("channels").getAsJsonObject()
									.get(address.getThingId()).getAsJsonObject()
									.addProperty(address.getChannelId(), value);
						}
					}
				}
			}
		}
		return j;
	}

	private String toChannelAddressList(JsonObject channels) throws OpenemsException {
		ArrayList<String> channelAddresses = new ArrayList<>();
		for (Entry<String, JsonElement> entry : channels.entrySet()) {
			String thingId = entry.getKey();
			JsonArray channelIds = JsonUtils.getAsJsonArray(entry.getValue());
			for (JsonElement channelElement : channelIds) {
				String channelId = JsonUtils.getAsString(channelElement);
				channelAddresses
						.add("MEAN(\"" + thingId + "/" + channelId + "\") AS \"" + thingId + "/" + channelId + "\"");
			}
		}
		return String.join(", ", channelAddresses);
	}
}
