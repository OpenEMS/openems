package io.openems.shared.influxdb;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.Tag;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

public class InfluxConnector {

	public final static String MEASUREMENT = "data";

	private final String ip;
	private final int port;
	private final String username;
	private final String password;
	private final String database;

	public InfluxConnector(String ip, int port, String username, String password, String database) {
		super();
		this.ip = ip;
		this.port = port;
		this.username = username;
		this.password = password;
		this.database = database;
	}

	private InfluxDB _influxDB = null;

	public String getDatabase() {
		return database;
	}

	/**
	 * Get InfluxDB Connection
	 * 
	 * @return
	 */
	public InfluxDB getConnection() throws OpenemsException {
		if (this._influxDB == null) {
			try {
				InfluxDB influxDB = InfluxDBFactory.connect("http://" + this.ip + ":" + this.port, this.username,
						this.password);
				// TODO try to create database
				influxDB.setDatabase(this.database);
				influxDB.enableBatch(BatchOptions.DEFAULTS);
				this._influxDB = influxDB;
			} catch (RuntimeException e) {
				throw new OpenemsException("Unable to connect to InfluxDB: " + e.getMessage(), e);
			}
		}
		return this._influxDB;
	}

	public void deactivate() {
		if (this._influxDB != null) {
			this._influxDB.close();
		}
	}

	/**
	 * copied from backend.timedata.influx.provider
	 * 
	 * @param influxdb
	 * @param database
	 * @param query
	 * @return
	 * @throws OpenemsException
	 */
	public QueryResult executeQuery(String query) throws OpenemsException {
		InfluxDB influxDB = this.getConnection();

		// Parse result
		QueryResult queryResult;
		try {
			queryResult = influxDB.query(new Query(query, this.database), TimeUnit.MILLISECONDS);
		} catch (RuntimeException e) {
			throw new OpenemsException("InfluxDB query runtime error. Query: " + query + ", Error: " + e.getMessage());
		}
		if (queryResult.hasError()) {
			throw new OpenemsException("InfluxDB query error. Query: " + query + ", Error: " + queryResult.getError());
		}
		return queryResult;
	}

	/**
	 * copied from backend.timedata.influx.provider
	 * 
	 * @param influxdb
	 * @param database
	 * @param influxIdOpt
	 * @param fromDate
	 * @param toDate
	 * @param channels
	 * @param resolution
	 * @return
	 * @throws OpenemsException
	 */
	public JsonArray queryHistoricData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution, Tag... tags) throws OpenemsException {
		// Prepare query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(Utils.toChannelAddressList(channels));
		query.append(" FROM data WHERE ");
		for (Tag tag : tags) {
			query.append(tag.getName() + " = '" + tag.getValue() + "' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.toEpochSecond()));
		query.append("s");
		query.append(" GROUP BY time(");
		query.append(resolution);
		query.append("s) fill(null)");

		QueryResult queryResult = executeQuery(query.toString());

		JsonArray j = new JsonArray();
		for (Result result : queryResult.getResults()) {
			List<Series> seriess = result.getSeries();
			if (seriess != null) {
				for (Series series : seriess) {
					// create thing/channel index
					ArrayList<ChannelAddress> addressIndex = new ArrayList<>();
					for (String column : series.getColumns()) {
						if (column.equals("time")) {
							continue;
						}
						addressIndex.add(ChannelAddress.fromString(column));
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
							ChannelAddress address = addressIndex.get(columnIndex - 1);
							j.get(timeIndex).getAsJsonObject().get("channels").getAsJsonObject()
									.get(address.getComponentId()).getAsJsonObject()
									.addProperty(address.getChannelId(), value);
						}
					}
				}
			}
		}
		return j;
	}

}
