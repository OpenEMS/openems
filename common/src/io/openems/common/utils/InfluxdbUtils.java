package io.openems.common.utils;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;

public class InfluxdbUtils {

	public static JsonArray queryHistoricData(InfluxDB influxdb, String database, Optional<Integer> deviceId,
			ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels, int resolution) throws OpenemsException {
		// Prepare query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(toChannelAddressList(channels));
		query.append(" FROM data WHERE ");
		if (deviceId.isPresent()) {
			query.append("fems = '");
			query.append(deviceId.get());
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
		query.append("s) fill(null)");

		QueryResult queryResult = executeQuery(influxdb, database, query.toString());

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

//	private static JsonObject querykWh(InfluxDB influxdb, String database, Optional<Integer> fems,
//			ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels, int resolution, JsonObject kWh)
//			throws OpenemsException {
//		JsonArray gridThing = getGridThing(kWh);
//		JsonArray storageThing = getStorageThing(kWh);
//		JsonArray things = new JsonArray();
//		things.addAll(storageThing);
//		things.addAll(gridThing);
//
//		JsonObject jThing = new JsonObject();
//		ArrayList<String> productionChannels = toChannelAddressListAvg(channels, things);
//
//		for (int i = 0; i < productionChannels.size(); i++) {
//			/*
//			 * SUM data
//			 */
//			StringBuilder query = new StringBuilder("SELECT SUM(AP) FROM (SELECT MEAN(\"");
//			query.append(productionChannels.get(i));
//			query.append("\") AS AP FROM data WHERE ");
//			if (fems.isPresent()) {
//				query.append("fems = '");
//				query.append(fems.get());
//				query.append("' AND ");
//			}
//			query.append("time > ");
//			query.append(String.valueOf(fromDate.toEpochSecond()));
//			query.append("s");
//			query.append(" AND time < ");
//			query.append(String.valueOf(toDate.toEpochSecond()));
//			query.append("s");
//			query.append(" GROUP BY time(1s) fill(previous))");
//
//			QueryResult queryResult = executeQuery(influxdb, database, query.toString());
//
//			Double sumProduction = 0.0;
//			try {
//				for (Result result : queryResult.getResults()) {
//					for (Series serie : result.getSeries()) {
//						for (List<Object> l : serie.getValues()) {
//							sumProduction = (Double) l.get(1);
//						}
//					}
//				}
//			} catch (Exception e) {
//				System.out.println("Error parsing SUM production: " + e);
//			}
//
//			/*
//			 * FIRST production data
//			 */
//			query = new StringBuilder("SELECT FIRST(\"");
//			query.append(productionChannels.get(i));
//			query.append("\") FROM data WHERE ");
//			if (fems.isPresent()) {
//				query.append("fems = '");
//				query.append(fems.get());
//				query.append("' AND ");
//			}
//			query.append("time > ");
//			query.append(String.valueOf(fromDate.toEpochSecond()));
//			query.append("s");
//			query.append(" AND time < ");
//			query.append(String.valueOf(toDate.toEpochSecond()));
//			query.append("s");
//
//			queryResult = executeQuery(influxdb, database, query.toString());
//
//			int second = 0;
//			try {
//				for (Result result : queryResult.getResults()) {
//					for (Series serie : result.getSeries()) {
//						for (List<Object> l : serie.getValues()) {
//							Instant timestampInstant = Instant.ofEpochMilli((long) ((Double) l.get(0)).doubleValue());
//							ZonedDateTime timestamp = ZonedDateTime.ofInstant(timestampInstant, fromDate.getZone());
//							if (timestamp.equals(fromDate)) {
//								System.out.println("Parsing FIRST: nothing null");
//							} else {
//								second = timestamp.getSecond();
//							}
//						}
//					}
//				}
//			} catch (Exception e) {
//				System.out.println("Error parsing FIRST production: " + e);
//			}
//
//			/*
//			 * LAST data
//			 */
//			query = new StringBuilder("SELECT LAST(\"");
//			query.append(productionChannels.get(i));
//			query.append("\") FROM data WHERE ");
//			if (fems.isPresent()) {
//				query.append("fems = '");
//				query.append(fems.get());
//				query.append("' AND ");
//			}
//			query.append("time < ");
//			query.append(String.valueOf(fromDate.toEpochSecond()));
//			query.append("s");
//
//			queryResult = executeQuery(influxdb, query.toString(), database);
//
//			try {
//				if (queryResult.getResults() != null) {
//					for (Result result : queryResult.getResults()) {
//						if (result.getSeries() != null) {
//							for (Series serie : result.getSeries()) {
//								if (serie.getValues() != null) {
//									for (List<Object> l : serie.getValues()) {
//										if (l.get(1) != null) {
//											sumProduction += (Double) l.get(1) * second;
//										}
//									}
//								}
//							}
//						}
//					}
//				}
//			} catch (Exception e) {
//				System.out.println("Error parsing LAST production: " + e);
//			}
//
//			Double avg = sumProduction / 3600 / 1000;
//
//			JsonObject element = new JsonObject();
//			element.addProperty("value", avg);
//			element.addProperty("type", JsonUtils.getAsString(kWh.get(productionChannels.get(i))));
//			jThing.add(productionChannels.get(i).toString(), element);
//		}
//
//		return jThing;
//	}

//	private static JsonArray getGridThing(JsonObject kWh) throws OpenemsException {
//		JsonArray gridThing = new JsonArray();
//		for (Entry<String, JsonElement> entry : kWh.entrySet()) {
//			String thingId = entry.getKey();
//			if (JsonUtils.getAsString(entry.getValue()).equals("grid")) {
//				gridThing.add(thingId);
//			}
//		}
//		return gridThing;
//	}
//
//	private static JsonArray getStorageThing(JsonObject kWh) throws OpenemsException {
//		JsonArray storageThing = new JsonArray();
//		for (Entry<String, JsonElement> entry : kWh.entrySet()) {
//			String thingId = entry.getKey();
//			if (JsonUtils.getAsString(entry.getValue()).equals("storage")) {
//				storageThing.add(thingId);
//			}
//		}
//		return storageThing;
//	}
//
//	private static ArrayList<String> toChannelAddressListAvg(JsonObject channels, JsonArray things)
//			throws OpenemsException {
//		ArrayList<String> channelAddresses = new ArrayList<>();
//		for (Entry<String, JsonElement> entry : channels.entrySet()) {
//			String thingId = entry.getKey();
//			JsonArray channelIds = JsonUtils.getAsJsonArray(entry.getValue());
//			for (JsonElement channelElement : channelIds) {
//				String channelId = JsonUtils.getAsString(channelElement);
//				if (channelId.contains("ActivePower")) {
//					String name = thingId + "/" + channelId;
//					boolean isGridOrStorage = false;
//					for (int i = 0; i < things.size(); i++) {
//						if (JsonUtils.getAsString(things.get(i)).equals(name)) {
//							isGridOrStorage = true;
//						}
//					}
//					if (!isGridOrStorage) {
//						channelAddresses.add(thingId + "/" + channelId);
//					}
//				}
//			}
//		}
//		return channelAddresses;
//	}

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

	private static QueryResult executeQuery(InfluxDB influxdb, String database, String query) throws OpenemsException {
		// Parse result
		QueryResult queryResult;
		try {
			queryResult = influxdb.query(new Query(query, database), TimeUnit.MILLISECONDS);
		} catch (RuntimeException e) {
			throw new OpenemsException("InfluxDB query runtime error. Query: " + query + ", Error: " + e.getMessage());
		}
		if (queryResult.hasError()) {
			throw new OpenemsException("InfluxDB query error. Query: " + query + ", Error: " + queryResult.getError());
		}
		return queryResult;
	}
}
