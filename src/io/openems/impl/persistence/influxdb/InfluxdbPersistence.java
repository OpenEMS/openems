/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.persistence.influxdb;

import java.net.Inet4Address;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
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

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.persistence.QueryablePersistence;
import io.openems.core.Address;
import io.openems.core.Databus;
import io.openems.core.utilities.JsonUtils;

@ThingInfo(title = "InfluxDB Persistence", description = "Persists data in an InfluxDB time-series database.")
public class InfluxdbPersistence extends QueryablePersistence implements ChannelUpdateListener {

	/*
	 * Config
	 */
	@ConfigInfo(title = "FEMS", description = "Sets FEMS-number.", type = Integer.class)
	public final ConfigChannel<Integer> fems = new ConfigChannel<>("fems", this);

	@ConfigInfo(title = "IP address", description = "IP address of InfluxDB.", type = Inet4Address.class)
	public final ConfigChannel<Inet4Address> ip = new ConfigChannel<>("ip", this);

	@ConfigInfo(title = "Username", description = "Username for InfluxDB.", type = String.class, defaultValue = "root")
	public final ConfigChannel<String> username = new ConfigChannel<>("username", this);

	@ConfigInfo(title = "Password", description = "Password for InfluxDB.", type = String.class, defaultValue = "root")
	public final ConfigChannel<String> password = new ConfigChannel<>("password", this);

	private ConfigChannel<Integer> cycleTime = new ConfigChannel<Integer>("cycleTime", this).defaultValue(10000);

	@Override
	public ConfigChannel<Integer> cycleTime() {
		return cycleTime;
	}

	/*
	 * Fields
	 */
	private final String DB_NAME = "db";
	private Optional<InfluxDB> _influxdb = Optional.empty();
	private HashMultimap<Long, FieldValue<?>> queue = HashMultimap.create();

	/*
	 * Methods
	 */
	/**
	 * Receives events for all {@link ReadChannel}s, excluding {@link ConfigChannel}s via the {@link Databus}.
	 */
	@Override
	public void channelUpdated(Channel channel, Optional<?> newValue) {
		if (!(channel instanceof ReadChannel<?>)) {
			return;
		}
		ReadChannel<?> readChannel = (ReadChannel<?>) channel;
		if (!newValue.isPresent()) {
			return;
		}
		Object value = newValue.get();
		String field = readChannel.address();
		FieldValue<?> fieldValue;
		if (value instanceof Number) {
			fieldValue = new NumberFieldValue(field, (Number) value);
		} else if (value instanceof String) {
			fieldValue = new StringFieldValue(field, (String) value);
		} else {
			return;
		}
		// Round time to Cycle-Time
		int cycleTime = this.cycleTime().valueOptional().get();
		Long timestamp = System.currentTimeMillis() / cycleTime * cycleTime;
		synchronized (queue) {
			queue.put(timestamp, fieldValue);
		}
	}

	@Override
	protected void dispose() {

	}

	@Override
	protected void forever() {
		// Prepare DB connection
		Optional<InfluxDB> _influxdb = getInfluxDB();
		if (!_influxdb.isPresent()) {
			synchronized (queue) {
				// Clear queue if we don't have a valid influxdb connection. This is necessary to avoid filling the
				// memory in case of no available DB connection
				queue.clear();
			}
		}
		InfluxDB influxDB = _influxdb.get();
		/*
		 * Convert FieldVales in queue to Points
		 */
		BatchPoints batchPoints = BatchPoints.database(DB_NAME) //
				.tag("fems", String.valueOf(fems.valueOptional().get())) //
				/* .retentionPolicy("autogen") */.build();
		synchronized (queue) {
			queue.asMap().forEach((timestamp, fieldValues) -> {
				Builder builder = Point.measurement("data") //
						.time(timestamp, TimeUnit.MILLISECONDS);
				fieldValues.forEach(fieldValue -> {
					if (fieldValue instanceof NumberFieldValue) {
						builder.addField(fieldValue.field, ((NumberFieldValue) fieldValue).value);
					} else if (fieldValue instanceof StringFieldValue) {
						builder.addField(fieldValue.field, ((StringFieldValue) fieldValue).value);
					}
				});
				batchPoints.point(builder.build());
			});
			queue.clear();
		}
		// write to DB
		influxDB.write(batchPoints);
		log.debug("Wrote [" + batchPoints.getPoints().size() + "] points to InfluxDB");
	}

	@Override
	protected boolean initialize() {
		if (getInfluxDB().isPresent()) {
			return true;
		} else {
			return false;
		}
	}

	private Optional<InfluxDB> getInfluxDB() {
		if (!this.ip.valueOptional().isPresent() || !this.fems.valueOptional().isPresent()
				|| !this.username.valueOptional().isPresent() || !this.password.valueOptional().isPresent()) {
			return Optional.empty();
		}

		if (_influxdb.isPresent()) {
			return this._influxdb;
		}

		String ip = this.ip.valueOptional().get().getHostAddress();
		String username = this.username.valueOptional().get();
		String password = this.password.valueOptional().get();

		InfluxDB influxdb = InfluxDBFactory.connect("http://" + ip + ":8086", username, password);
		try {
			influxdb.createDatabase(DB_NAME);
		} catch (RuntimeException e) {
			log.error("Unable to connect to InfluxDB: " + e.getCause());
			return Optional.empty();
		}

		this._influxdb = Optional.of(influxdb);
		return this._influxdb;
	}

	@Override
	public JsonObject query(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels, int resolution,
			JsonObject kWh) throws OpenemsException {

		JsonObject jQueryreply = new JsonObject();
		jQueryreply.addProperty("mode", "history");
		JsonArray jData = this.queryData(fromDate, toDate, channels, resolution);
		jQueryreply.add("data", jData);
		JsonObject jkWh = this.querykWh(fromDate, toDate, channels, resolution, kWh);
		jQueryreply.add("kWh", jkWh);

		return jQueryreply;
	}

	private JsonObject querykWh(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels, int resolution,
			JsonObject kWh) throws OpenemsException {
		log.info(kWh.get("ess0/ActivePower").toString());
		String gridThing = this.getGridThing(kWh);
		String storageThing = this.getStorageThing(kWh);

		// Prepare kWh query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(toChannelAddressListAvg(channels));
		query.append(" FROM data WHERE ");
		if (fems.valueOptional().isPresent()) {
			query.append("fems = '");
			query.append(fems.valueOptional().get());
			query.append("' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.plusDays(1).toEpochSecond()));
		query.append("s");
		query.append(" AND \"" + gridThing + "\" >= 0");
		query.append(" AND \"" + storageThing + "\" >= 0");
		log.info(query.toString());

		QueryResult queryResult = this.executeQuery(query.toString());

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
									this.querySellToGrid(fromDate, toDate, channels, resolution, gridThing));
							element.addProperty("type",
									JsonUtils.getAsString(kWh.get(addressIndex.get(columnIndex).toString())));
						} else if (addressIndex.get(columnIndex).toString().equals(storageThing)) {
							element.addProperty("charge", (Double) serie.getValues().get(0).get(columnIndex + 1));
							element.addProperty("discharge",
									this.queryDischargeStorage(fromDate, toDate, channels, resolution, gridThing));
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

	private double querySellToGrid(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels, int resolution,
			String gridThing) throws OpenemsException {
		// Prepare SellToGrid query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append("MEAN(\"" + gridThing + "\") * 24 / 1000 as \"" + gridThing + "\"");
		query.append(" FROM data WHERE ");
		if (fems.valueOptional().isPresent()) {
			query.append("fems = '");
			query.append(fems.valueOptional().get());
			query.append("' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.plusDays(1).toEpochSecond()));
		query.append("s");
		query.append(" AND \"" + gridThing + "\" <= 0");

		QueryResult queryResult = this.executeQuery(query.toString());

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

	private double queryDischargeStorage(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels,
			int resolution, String storageThing) throws OpenemsException {
		// Prepare SellToGrid query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append("MEAN(\"" + storageThing + "\") * 24 / 1000 as \"" + storageThing + "\"");
		query.append(" FROM data WHERE ");
		if (fems.valueOptional().isPresent()) {
			query.append("fems = '");
			query.append(fems.valueOptional().get());
			query.append("' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.plusDays(1).toEpochSecond()));
		query.append("s");
		query.append(" AND \"" + storageThing + "\" <= 0");

		QueryResult queryResult = this.executeQuery(query.toString());

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

	private JsonArray queryData(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels, int resolution)
			throws OpenemsException {
		// Prepare query string
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(toChannelAddressList(channels));
		query.append(" FROM data WHERE ");
		if (fems.valueOptional().isPresent()) {
			query.append("fems = '");
			query.append(fems.valueOptional().get());
			query.append("' AND ");
		}
		query.append("time > ");
		query.append(String.valueOf(fromDate.toEpochSecond()));
		query.append("s");
		query.append(" AND time < ");
		query.append(String.valueOf(toDate.plusDays(1).toEpochSecond()));
		query.append("s");
		query.append(" GROUP BY time(");
		query.append(resolution);
		query.append("s)");
		log.info(query.toString());

		QueryResult queryResult = this.executeQuery(query.toString());

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

	private String toChannelAddressList(JsonObject channels) throws ReflectionException {
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

	private String toChannelAddressListAvg(JsonObject channels) throws ReflectionException {
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

	private String getGridThing(JsonObject kWh) throws ReflectionException {
		String gridThing = "";
		for (Entry<String, JsonElement> entry : kWh.entrySet()) {
			String thingId = entry.getKey();
			if (JsonUtils.getAsString(entry.getValue()).equals("grid")) {
				gridThing = thingId;
			}
		}
		return gridThing;
	}

	private String getStorageThing(JsonObject kWh) throws ReflectionException {
		String storageThing = "";
		for (Entry<String, JsonElement> entry : kWh.entrySet()) {
			String thingId = entry.getKey();
			if (JsonUtils.getAsString(entry.getValue()).equals("storage")) {
				storageThing = thingId;
			}
		}
		return storageThing;
	}

	private QueryResult executeQuery(String query) throws OpenemsException {
		// Prepare DB connection
		Optional<InfluxDB> _influxdb = getInfluxDB();
		if (!_influxdb.isPresent()) {
			throw new OpenemsException("Unable to connect to InfluxDB.");
		}
		InfluxDB influxDB = _influxdb.get();

		// Parse result
		QueryResult queryResult = influxDB.query(new Query(query, DB_NAME), TimeUnit.MILLISECONDS);
		if (queryResult.hasError()) {
			throw new OpenemsException("InfluxDB query error: " + queryResult.getError());
		}

		return queryResult;
	}

}
