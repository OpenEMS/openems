package io.openems.impl.persistence.influxdb;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
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

import io.openems.api.exception.OpenemsException;
import io.openems.core.Address;
import io.openems.core.utilities.JsonUtils;

public class InfluxdbQueryWrapper {

	private final static Logger log = LoggerFactory.getLogger(InfluxdbQueryWrapper.class);
	private final static String DB_NAME = "db";

	public static JsonObject query(Optional<InfluxDB> _influxdb, Optional<Integer> fems, ZonedDateTime fromDate,
			ZonedDateTime toDate, JsonObject channels, int resolution, JsonObject kWh) throws OpenemsException {
		// Prepare return object
		JsonObject jQueryreply = new JsonObject();
		jQueryreply.addProperty("mode", "history");
		JsonArray jData;
		JsonObject jkWh;

		// Prepare date
		toDate = toDate.plusDays(1).truncatedTo(ChronoUnit.DAYS);

		ZonedDateTime nowDate = ZonedDateTime.now();
		if (nowDate.isBefore(toDate)) {
			toDate = nowDate;
		}
		if (fromDate.isAfter(toDate)) {
			fromDate = toDate;
		}

		if (_influxdb.isPresent()) {
			InfluxDB influxdb = _influxdb.get();
			jData = InfluxdbQueryWrapper.queryData(influxdb, fems, fromDate, toDate, channels, resolution);
			jkWh = InfluxdbQueryWrapper.querykWh(influxdb, fems, fromDate, toDate, channels, resolution, kWh);
		} else {
			jData = new JsonArray();
			jkWh = new JsonObject();
		}
		jQueryreply.add("data", jData);
		jQueryreply.add("kWh", jkWh);
		return jQueryreply;
	}

	private static JsonArray queryData(InfluxDB influxdb, Optional<Integer> fems, ZonedDateTime fromDate,
			ZonedDateTime toDate, JsonObject channels, int resolution) throws OpenemsException {
		// Prepare query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(toChannelAddressList(channels));
		query.append(" FROM data WHERE ");
		if (fems.isPresent()) {
			query.append("fems = '");
			query.append(fems.get());
			query.append("' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.toEpochSecond()));
		query.append("s");
		query.append(" GROUP BY time(");
		query.append(resolution);
		query.append("s) fill(previous)");
		log.info(query.toString());

		QueryResult queryResult = executeQuery(influxdb, query.toString());

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

	private static JsonObject querykWh(InfluxDB influxdb, Optional<Integer> fems, ZonedDateTime fromDate,
			ZonedDateTime toDate, JsonObject channels, int resolution, JsonObject kWh) throws OpenemsException {
		String gridThing = getGridThing(kWh);
		String storageThing = getStorageThing(kWh);

		// Prepare kWh query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(toChannelAddressListAvg(channels));
		query.append(" FROM data WHERE ");
		if (fems.isPresent()) {
			query.append("fems = '");
			query.append(fems.get());
			query.append("' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.toEpochSecond()));
		query.append("s");
		query.append(" AND \"" + gridThing + "\" >= 0");
		query.append(" AND \"" + storageThing + "\" >= 0");
		log.info(query.toString());

		QueryResult queryResult = executeQuery(influxdb, query.toString());

		JsonArray j = new JsonArray();

		// AVG data
		JsonObject jThing = new JsonObject();
		for (Result result : queryResult.getResults()) {
			List<Series> series = result.getSeries();
			if (series != null) {
				for (Series serie : series) {
					ArrayList<Address> addressIndex = new ArrayList<>();
					for (String column : serie.getColumns()) {
						if (column.equals("time")) {
							continue;
						}
						addressIndex.add(Address.fromString(column));
					}
					for (int columnIndex = 0; columnIndex < addressIndex.size(); columnIndex++) {
						JsonObject element = new JsonObject();
						if (addressIndex.get(columnIndex).toString().equals(gridThing)) {
							element.addProperty("buy", (Double) serie.getValues().get(0).get(columnIndex + 1));
							element.addProperty("sell",
									querySellToGrid(influxdb, fems, fromDate, toDate, channels, resolution, gridThing));
							element.addProperty("type",
									JsonUtils.getAsString(kWh.get(addressIndex.get(columnIndex).toString())));
						} else if (addressIndex.get(columnIndex).toString().equals(storageThing)) {
							element.addProperty("charge", (Double) serie.getValues().get(0).get(columnIndex + 1));
							element.addProperty("discharge", queryDischargeStorage(influxdb, fems, fromDate, toDate,
									channels, resolution, gridThing));
							element.addProperty("type",
									JsonUtils.getAsString(kWh.get(addressIndex.get(columnIndex).toString())));
						} else {
							element.addProperty("value", (Double) serie.getValues().get(0).get(columnIndex + 1));
							element.addProperty("type",
									JsonUtils.getAsString(kWh.get(addressIndex.get(columnIndex).toString())));
						}
						jThing.add(addressIndex.get(columnIndex).toString(), element);
					}
				}
			}
		}
		j.add(jThing);

		return jThing;
	}

	private static double querySellToGrid(InfluxDB influxdb, Optional<Integer> fems, ZonedDateTime fromDate,
			ZonedDateTime toDate, JsonObject channels, int resolution, String gridThing) throws OpenemsException {
		// Prepare SellToGrid query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append("MEAN(\"" + gridThing + "\") * 24 / 1000 as \"" + gridThing + "\"");
		query.append(" FROM data WHERE ");
		if (fems.isPresent()) {
			query.append("fems = '");
			query.append(fems.get());
			query.append("' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.plusDays(1).toEpochSecond()));
		query.append("s");
		query.append(" AND \"" + gridThing + "\" <= 0");

		QueryResult queryResult = executeQuery(influxdb, query.toString());

		JsonArray j = new JsonArray();
		double value = 0;

		// AVG data
		JsonObject jThing = new JsonObject();
		for (Result result : queryResult.getResults()) {
			List<Series> series = result.getSeries();
			if (series != null) {
				for (Series serie : series) {
					ArrayList<Address> addressIndex = new ArrayList<>();
					for (String column : serie.getColumns()) {
						if (column.equals("time")) {
							continue;
						}
						addressIndex.add(Address.fromString(column));
					}
					for (int columnIndex = 0; columnIndex < addressIndex.size(); columnIndex++) {
						value = (Double) serie.getValues().get(0).get(columnIndex + 1);
					}
				}
			}
		}
		j.add(jThing);

		return value;
	}

	private static double queryDischargeStorage(InfluxDB influxdb, Optional<Integer> fems, ZonedDateTime fromDate,
			ZonedDateTime toDate, JsonObject channels, int resolution, String storageThing) throws OpenemsException {
		// Prepare SellToGrid query string
		StringBuilder query = new StringBuilder("SELECT ");
		// TODO calculate distance between fromDate and toDate
		query.append("MEAN(\"" + storageThing + "\") * 24 / 1000 as \"" + storageThing + "\"");
		query.append(" FROM data WHERE ");
		if (fems.isPresent()) {
			query.append("fems = '");
			query.append(fems.get());
			query.append("' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.plusDays(1).toEpochSecond()));
		query.append("s");
		query.append(" AND \"" + storageThing + "\" <= 0");

		QueryResult queryResult = executeQuery(influxdb, query.toString());

		JsonArray j = new JsonArray();
		double value = 0;

		// AVG data
		JsonObject jThing = new JsonObject();
		for (Result result : queryResult.getResults()) {
			List<Series> series = result.getSeries();
			if (series != null) {
				for (Series serie : series) {
					ArrayList<Address> addressIndex = new ArrayList<>();
					for (String column : serie.getColumns()) {
						if (column.equals("time")) {
							continue;
						}
						addressIndex.add(Address.fromString(column));
					}
					for (int columnIndex = 0; columnIndex < addressIndex.size(); columnIndex++) {
						value = (Double) serie.getValues().get(0).get(columnIndex + 1);
					}
				}
			}
		}
		j.add(jThing);

		return value;
	}

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

	private static QueryResult executeQuery(InfluxDB influxdb, String query) throws OpenemsException {
		// Parse result
		QueryResult queryResult = influxdb.query(new Query(query, DB_NAME), TimeUnit.MILLISECONDS);
		if (queryResult.hasError()) {
			throw new OpenemsException("InfluxDB query error: " + queryResult.getError());
		}

		return queryResult;
	}

	private static String getGridThing(JsonObject kWh) throws OpenemsException {
		String gridThing = "";
		for (Entry<String, JsonElement> entry : kWh.entrySet()) {
			String thingId = entry.getKey();
			if (JsonUtils.getAsString(entry.getValue()).equals("grid")) {
				gridThing = thingId;
			}
		}
		return gridThing;
	}

	private static String getStorageThing(JsonObject kWh) throws OpenemsException {
		String storageThing = "";
		for (Entry<String, JsonElement> entry : kWh.entrySet()) {
			String thingId = entry.getKey();
			if (JsonUtils.getAsString(entry.getValue()).equals("storage")) {
				storageThing = thingId;
			}
		}
		return storageThing;
	}

	private static String toChannelAddressListAvg(JsonObject channels) throws OpenemsException {
		ArrayList<String> channelAddresses = new ArrayList<>();
		for (Entry<String, JsonElement> entry : channels.entrySet()) {
			String thingId = entry.getKey();
			JsonArray channelIds = JsonUtils.getAsJsonArray(entry.getValue());
			for (JsonElement channelElement : channelIds) {
				String channelId = JsonUtils.getAsString(channelElement);
				if (channelId.equals("ActivePower")) {
					channelAddresses.add("MEAN(\"" + thingId + "/" + channelId + "\") * 24 / 1000 AS \"" + thingId + "/"
							+ channelId + "\"");
				}
			}
		}
		return String.join(", ", channelAddresses);
	}
}
