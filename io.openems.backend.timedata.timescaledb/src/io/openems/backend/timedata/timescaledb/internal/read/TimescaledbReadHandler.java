package io.openems.backend.timedata.timescaledb.internal.read;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonElement;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.timedata.timescaledb.Config;
import io.openems.backend.timedata.timescaledb.internal.Schema;
import io.openems.backend.timedata.timescaledb.internal.Utils;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.timedata.CommonTimedataService;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public class TimescaledbReadHandler {

	private final Logger log = LoggerFactory.getLogger(TimescaledbReadHandler.class);

	private final AtomicReference<Schema> schema = new AtomicReference<>();

	/**
	 * A {@link HikariDataSource} used solely for reads.
	 */
	private final HikariDataSource dataSource;

	public TimescaledbReadHandler(Config config) throws SQLException {
		this.dataSource = Utils.getDataSource(//
				config.host(), config.port(), config.database(), //
				config.user(), config.password(), config.poolSize());
	}

	/**
	 * Called by TimescaledbImpl deactivate().
	 */
	public void deactivate() {
		if (this.dataSource != null) {
			this.dataSource.close();
		}
	}

	public void setSchema(Schema schema) {
		this.schema.set(schema);
	}

	/**
	 * See
	 * {@link CommonTimedataService#queryHistoricData(String, ZonedDateTime, ZonedDateTime, Set, Resolution)}.
	 * 
	 * @param edgeId     the Edge-ID; or null query all
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the {@link Resolution}
	 * @return the query result
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		var channelStrings = toStringSet(channels);

		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		var result = Utils.prepareDataMap(fromDate, toDate, channels, resolution);
		var types = Utils.querySchemaCache(this.assertAndGetSchema(), edgeId, channelStrings);

		// Open ONE database connection
		try (var con = this.dataSource.getConnection()) {

			// Execute specific query for each Type
			for (var typeEntry : types.entrySet()) {
				final var type = typeEntry.getKey();
				for (var priorityEntry : typeEntry.getValue().entrySet()) {

					final var priority = priorityEntry.getKey();
					final var ids = priorityEntry.getValue();

					// Build custom SQL for PreparedStatement
					var sql = "SELECT" //
							+ "    time_bucket(" //
							+ "        ?::interval," // [1] Resolution
							+ "        data.time)," //
							+ "    data.edge_channel_id," //
							+ "    " + type.defaultAggregateFunction + "(data." + type.defaultAggregateFunction + ") " //
							+ "FROM " + type.getAggregate5mTableName(priority) + " data " //
							+ "WHERE" //
							+ "    data.edge_channel_id IN (" //
							+ ids.keySet().stream() //
									.map(c -> "?") // [2++] Channel-ID
									.collect(Collectors.joining(",")) //
							+ "    ) AND" //
							+ "    data.time >= ? AND" // [n-1] FromDate
							+ "    data.time < ? " // [n] ToDate
							+ "GROUP BY 1,2";

					// Query the database
					try (var pst = con.prepareStatement(sql)) {
						// Fill PreparedStatement.

						// Reference for Java 8 Date and Time classes with PostgreSQL:
						// https://jdbc.postgresql.org/documentation/query/#using-java-8-date-and-time-classes
						var i = 1;
						pst.setString(i++, Utils.toSqlInterval(resolution));
						for (var id : ids.keySet()) {
							pst.setInt(i++, id);
						}
						pst.setObject(i++, fromDate.toOffsetDateTime());
						pst.setObject(i++, toDate.toOffsetDateTime());

						var rs = pst.executeQuery();
						while (rs.next()) {
							var time = rs.getObject(1, OffsetDateTime.class).atZoneSameInstant(fromDate.getZone());
							var channelAddress = ChannelAddress.fromString(ids.get(rs.getInt(2)));
							var value = type.parseValueFromResultSet(rs, 3);
							var resultTime = result.computeIfAbsent(time, t -> new TreeMap<>());
							resultTime.put(channelAddress, value);
						}

					} catch (SQLException e) {
						this.log.error(
								"Unable to query historic data for type [" + type.name() + "]: " + e.getMessage());
						// TODO collect exceptions; throw error if everything fails
					}
				}
			}
		} catch (SQLException e) {
			this.log.error("Unable to query historic data: " + e.getMessage());
			throw new OpenemsException("Error while querying historic data");
		}
		return result;
	}

	/**
	 * See
	 * {@link CommonTimedataService#queryHistoricEnergy(String, ZonedDateTime, ZonedDateTime, Set)}.
	 * 
	 * @param edgeId   the Edge-ID; or null query all
	 * @param fromDate the From-Date
	 * @param toDate   the To-Date
	 * @param channels the Channels
	 * @return the query result
	 */
	public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels) throws OpenemsNamedException {
		var channelStrings = toStringSet(channels);

		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		var result = Utils.prepareEnergyMap(fromDate, toDate, channels);
		var types = Utils.querySchemaCache(this.assertAndGetSchema(), edgeId, channelStrings);

		// Open ONE database connection
		try (var con = this.dataSource.getConnection()) {

			// Execute specific query for each Type
			for (var typeEntry : types.entrySet()) {
				final var type = typeEntry.getKey();
				for (var priorityEntry : typeEntry.getValue().entrySet()) {

					final var priority = priorityEntry.getKey();
					final var ids = priorityEntry.getValue();

					// Build custom SQL for PreparedStatement
					var sql = "SELECT" //
							+ "	   d.edge_channel_id," //
							+ "    LAST(\"max\", d.time) - FIRST(\"min\", d.time) " //
							+ "FROM " + type.getAggregate5mTableName(priority) + " d " //
							+ "WHERE" //
							+ "    d.edge_channel_id = ANY (?) AND" // [1] Channel-ID
							+ "    d.time >= ? AND" // [2] FromDate
							+ "    d.time < ?" // [3] ToDate
							+ "    group by 1;";
					// Query the database
					try (var pst = con.prepareStatement(sql)) {
						// Fill PreparedStatement.
						// Reference for Java 8 Date and Time classes with PostgreSQL:
						// https://jdbc.postgresql.org/documentation/query/#using-java-8-date-and-time-classes
						var i = 1;
						pst.setArray(i++, con.createArrayOf("INTEGER", ids.keySet().toArray(Integer[]::new)));
						pst.setObject(i++, fromDate.toOffsetDateTime());
						pst.setObject(i++, toDate.toOffsetDateTime());

						var rs = pst.executeQuery();
						while (rs.next()) {
							var channelAddress = type.parseValueFromResultSet(rs, 1).getAsInt();
							var channel = ChannelAddress.fromString(ids.get(channelAddress));
							var value = type.parseValueFromResultSet(rs, 2);
							result.put(channel, value);
						}

					} catch (SQLException e) {
						this.log.error(
								"Unable to query historic energy for type [" + type.name() + "]: " + e.getMessage());
						// TODO collect exceptions; throw error if everything fails
					}
				}
			}
		} catch (SQLException e) {
			this.log.error("Unable to query historic energy: " + e.getMessage());
			throw new OpenemsException("Error while querying historic energy");
		}
		return result;
	}

	/**
	 * See
	 * {@link CommonTimedataService#queryHistoricEnergyPerPeriod(String, ZonedDateTime, ZonedDateTime, Set, Resolution)}.
	 * 
	 * @param edgeId     the Edge-ID; or null query all
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the {@link Resolution}
	 * @return the query result
	 */
	public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricEnergyPerPeriod(String edgeId,
			ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
			throws OpenemsNamedException {
		var channelStrings = toStringSet(channels);

		// handle empty call
		if (channels.isEmpty()) {
			return new TreeMap<>();
		}

		var result = Utils.prepareDataMap(fromDate, toDate, channels, resolution);
		var types = Utils.querySchemaCache(this.assertAndGetSchema(), edgeId, channelStrings);

		// Open ONE database connection
		try (var con = this.dataSource.getConnection()) {

			// Execute specific query for each Type
			for (var typeEntry : types.entrySet()) {
				final var type = typeEntry.getKey();
				for (var priorityEntry : typeEntry.getValue().entrySet()) {

					final var priority = priorityEntry.getKey();
					final var ids = priorityEntry.getValue();

					// Build custom SQL for PreparedStatement
					var sql = "SELECT" //
							+ "    timescaledb_experimental.time_bucket_ng(" //
							+ "        ?::interval," // [1] Resolution
							+ "        data.time," //
							+ "        timezone => ?)," // [2] timezone
							+ "    data.edge_channel_id," //
							+ "    LAST(\"max\", data.time)" //
							+ "FROM " + type.getAggregate5mTableName(priority) + " data " //
							+ "WHERE" //
							+ "    data.edge_channel_id = ANY (?) AND" // [3] Channel IDs
							+ "    data.time >= ? AND" // [4] FromDate
							+ "    data.time < ? " // [5] ToDate
							+ "GROUP BY 1,2";

					// Query the database
					var data = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();
					try (var pst = con.prepareStatement(sql)) {
						// Fill PreparedStatement.

						// Reference for Java 8 Date and Time classes with PostgreSQL:
						// https://jdbc.postgresql.org/documentation/query/#using-java-8-date-and-time-classes
						var i = 1;
						pst.setString(i++, Utils.toSqlInterval(resolution));
						pst.setString(i++, fromDate.getZone().getId());
						pst.setArray(i++, con.createArrayOf("INTEGER", ids.keySet().toArray(Integer[]::new)));
						pst.setObject(i++,
								fromDate.minus(resolution.getValue(), resolution.getUnit()).toOffsetDateTime());
						pst.setObject(i++, toDate.toOffsetDateTime());

						var rs = pst.executeQuery();
						while (rs.next()) {
							var time = rs.getObject(1, OffsetDateTime.class).atZoneSameInstant(fromDate.getZone());
							var channelAddress = ChannelAddress.fromString(ids.get(rs.getInt(2)));
							var value = type.parseValueFromResultSet(rs, 3);
							var dataTime = data.computeIfAbsent(time, t -> new TreeMap<>());
							dataTime.put(channelAddress, value);
						}

					} catch (SQLException e) {
						this.log.error(
								"Unable to query historic data for type [" + type.name() + "]: " + e.getMessage());
						// TODO collect exceptions; throw error if everything fails
					}

					// Calculate delta
					SortedMap<ChannelAddress, JsonElement> lastEntry = null;
					for (var entry : data.entrySet()) {
						if (lastEntry != null) { // ignore first entry with time t-1
							var time = entry.getKey();
							for (var id : ids.entrySet()) {
								var channelAddress = ChannelAddress.fromString(id.getValue());
								var lastValue = lastEntry.get(channelAddress);
								var thisValue = entry.getValue().get(channelAddress);
								var resultTime = result.computeIfAbsent(time, t -> new TreeMap<>());
								resultTime.put(channelAddress, type.subtract(thisValue, lastValue));
							}
						}
						lastEntry = entry.getValue();
					}
				}
			}
		} catch (SQLException e) {
			this.log.error("Unable to query historic data: " + e.getMessage());
			throw new OpenemsException("Error while querying historic data");
		}
		return result;
	}

	/**
	 * Gets the latest values for the given ChannelAddresses.
	 *
	 * @param edgeId   The unique Edge-ID
	 * @param channels The {@link ChannelAddress}es
	 * @return the values
	 */
	public Map<ChannelAddress, JsonElement> getChannelValues(String edgeId, Set<ChannelAddress> channels) {
		// TODO
		return Collections.emptyMap();
	}

	/**
	 * Gets the Schema, never null. Throws an {@link OpenemsException} if the Schema
	 * has not been loaded yet.
	 * 
	 * @return {@link Schema}
	 * @throws OpenemsException on error
	 */
	private Schema assertAndGetSchema() throws OpenemsException {
		var result = this.schema.get();
		if (result == null) {
			throw new OpenemsException("Database Schema is not available yet");
		}
		return result;
	}

	/**
	 * Returns a DebugMetrics map.
	 * 
	 * @return metrics
	 */
	public Map<String, Number> debugMetrics() {
		var data = new HashMap<String, Number>();
		try (//
				var con = this.dataSource.getConnection(); //
				var st = con.createStatement() //
		) {
			var rs = st.executeQuery("" //
					+ "SELECT"
					+ "    hypertable_name, hypertable_size(format('%I.%I', hypertable_schema, hypertable_name)::regclass) / 1024 / 1024 "
					+ "FROM timescaledb_information.hypertables");
			while (rs.next()) {
				var tableName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, rs.getString(1));
				var size = rs.getInt(2);
				data.put(tableName, size);
			}

		} catch (SQLException e) {
			this.log.warn("Unable to query debugMetrics: " + e.getMessage());
		}
		return data;
	}

	private static Set<String> toStringSet(Set<ChannelAddress> channels) {
		return channels.stream().map(c -> c.toString()).collect(Collectors.toUnmodifiableSet());
	}
}
