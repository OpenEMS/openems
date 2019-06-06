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
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.persistence.QueryablePersistence;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.ChannelEnum;
import io.openems.core.Databus;

@ThingInfo(title = "InfluxDB Persistence", description = "Persists data in an InfluxDB time-series database.")
public class InfluxdbPersistence extends QueryablePersistence implements ChannelUpdateListener {

	private final Logger log = LoggerFactory.getLogger(InfluxdbPersistence.class);

	private ThingStateChannels thingState;

	/*
	 * Config
	 */
	@ChannelInfo(title = "FEMS", description = "Sets FEMS-number.", type = Integer.class)
	public final ConfigChannel<Integer> fems = new ConfigChannel<>("fems", this);

	@ChannelInfo(title = "IP address", description = "IP address of InfluxDB.", type = Inet4Address.class)
	public final ConfigChannel<Inet4Address> ip = new ConfigChannel<>("ip", this);

	@ChannelInfo(title = "Username", description = "Username for InfluxDB.", type = String.class, defaultValue = "root")
	public final ConfigChannel<String> username = new ConfigChannel<>("username", this);

	@ChannelInfo(title = "Password", description = "Password for InfluxDB.", type = String.class, defaultValue = "root")
	public final ConfigChannel<String> password = new ConfigChannel<>("password", this);

	@ChannelInfo(title = "Database", description = "Database name for InfluxDB.", type = String.class, defaultValue = "db")
	public final ConfigChannel<String> database = new ConfigChannel<>("database", this);

	@ChannelInfo(title = "Sets the duration of each cycle in milliseconds", type = Integer.class)
	public ConfigChannel<Integer> cycleTime = new ConfigChannel<Integer>("cycleTime", this).defaultValue(10000);

	public InfluxdbPersistence() {
		this.thingState = new ThingStateChannels(this);
	}

	/*
	 * Fields
	 */
	private static final int DEFAULT_CYCLETIME = 10000;
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
		String field = readChannel.address().toString();
		FieldValue<?> fieldValue;
		// TODO merge this with io.openems.backend.timedata.influx.addChannelToBuilder()
		if (value instanceof Number) {
			fieldValue = new NumberFieldValue(field, (Number) value);
		} else if (value instanceof String) {
			fieldValue = new StringFieldValue(field, (String) value);
		} else if (value instanceof ChannelEnum) {
			fieldValue = new NumberFieldValue(field, ((ChannelEnum) value).getValue());
		} else {
			return;
		}
		// Round time to Cycle-Time
		int cycleTime = this.getCycleTime();
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
		BatchPoints batchPoints = BatchPoints.database(database.valueOptional().orElse("db")) //
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
		try {
			influxDB.write(batchPoints);
			log.debug("Wrote [" + batchPoints.getPoints().size() + "] points to InfluxDB");
		} catch (RuntimeException e) {
			log.error("Error writing to InfluxDB: " + e);
		}
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
			influxdb.createDatabase(database.valueOptional().orElse("db"));
		} catch (RuntimeException e) {
			log.error("Unable to connect to InfluxDB: " + e.getCause());
			return Optional.empty();
		}

		this._influxdb = Optional.of(influxdb);
		return this._influxdb;
	}

	@Override
	public JsonArray queryHistoricData(Optional<Integer> edgeIdOpt, ZonedDateTime fromDate, ZonedDateTime toDate,
			JsonObject channels, int resolution) throws io.openems.common.exceptions.OpenemsException {
		Optional<InfluxDB> influxdbOpt = getInfluxDB();
		if (!influxdbOpt.isPresent()) {
			throw new OpenemsException("InfluxDB is not available");
		}
		Optional<String> databaseOpt = this.database.valueOptional();
		if (!databaseOpt.isPresent()) {
			throw new OpenemsException("InfluxDB database is not available");
		}
		return InfluxdbUtils.queryHistoricData(influxdbOpt.get(), databaseOpt.get(), edgeIdOpt, fromDate, toDate,
				channels, resolution);
	}

	@Override
	protected int getCycleTime() {
		return cycleTime.valueOptional().orElse(DEFAULT_CYCLETIME);
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

	@Override
	public Optional<Object> getChannelValue(int edgeId, ChannelAddress channelAddress) {
		log.error("getChannelValue is not implemented");
		return Optional.empty();
	}

	@Override
	public void write(int edgeId, JsonObject jData) throws OpenemsException {
		throw new OpenemsException("write is not implemented");
	}
}