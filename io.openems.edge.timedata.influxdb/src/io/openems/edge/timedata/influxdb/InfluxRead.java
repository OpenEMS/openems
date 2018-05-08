package io.openems.edge.timedata.influxdb;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

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
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.timedata.influxdb.InfluxTimedata.ChannelId;

public class InfluxRead {

	// private final Logger log = LoggerFactory.getLogger(InfluxRead.class);

	private final InfluxTimedata parent;

	private InfluxDB _influxDB = null;

	public InfluxRead(InfluxTimedata parent) {
		this.parent = parent;
	}

	/**
	 * Get InfluxDB Connection (org.influxdb)
	 * 
	 * @return
	 */
	protected InfluxDB getConnection() throws OpenemsException {
		if (this._influxDB == null) {
			try {
				org.influxdb.InfluxDB influxDB = InfluxDBFactory.connect(
						"http://" + this.parent.ip + ":" + this.parent.port, this.parent.username,
						this.parent.password);
				influxDB.createDatabase(this.parent.database);
				this._influxDB = influxDB;
				this.parent.channel(ChannelId.STATE_1).setNextValue(false);
			} catch (RuntimeException e) {
				this.parent.channel(ChannelId.STATE_1).setNextValue(true);
				throw new OpenemsException("Unable to connect to InfluxDB (read): " + e.getMessage(), e);
			}
		}
		return this._influxDB;
	}

	/**
	 * TODO: copied from backend.timedata.influx.provider
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
			int resolution) throws OpenemsException {
		// Prepare query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(toChannelAddressList(channels));
		query.append(" FROM data WHERE ");
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
									.get(address.getThingId()).getAsJsonObject()
									.addProperty(address.getChannelId(), value);
						}
					}
				}
			}
		}
		return j;
	}

	/**
	 * TODO: copied from backend.timedata.influx.provider
	 * 
	 * @param channels
	 * @return
	 * @throws OpenemsException
	 */
	private static String toChannelAddressList(JsonObject channels) throws OpenemsException {
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

	/**
	 * TODO: copied from backend.timedata.influx.provider
	 * 
	 * @param influxdb
	 * @param database
	 * @param query
	 * @return
	 * @throws OpenemsException
	 */
	private QueryResult executeQuery(String query) throws OpenemsException {
		InfluxDB influxDB = this.getConnection();

		// Parse result
		QueryResult queryResult;
		try {
			queryResult = influxDB.query(new Query(query, this.parent.database), TimeUnit.MILLISECONDS);
		} catch (RuntimeException e) {
			throw new OpenemsException("InfluxDB query runtime error. Query: " + query + ", Error: " + e.getMessage());
		}
		if (queryResult.hasError()) {
			throw new OpenemsException("InfluxDB query error. Query: " + query + ", Error: " + queryResult.getError());
		}
		return queryResult;
	}
}
