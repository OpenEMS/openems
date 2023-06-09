package io.openems.backend.timedata.timescaledb.internal;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.postgresql.Driver;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.timedata.timescaledb.TimedataTimescaleDb;
import io.openems.backend.timedata.timescaledb.internal.Schema.ChannelRecord;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public class Utils {

	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private Utils() {

	}

	/**
	 * Creates a {@link HikariDataSource} connection pool.
	 *
	 * @param host     the database hostname
	 * @param port     the database port
	 * @param database the database name
	 * @param user     the database user
	 * @param password the database password
	 * @param poolSize the pool size
	 * @return the HikariDataSource
	 * @throws SQLException on error
	 */
	public static HikariDataSource getDataSource(String host, int port, String database, String user, String password,
			int poolSize) throws SQLException {
		if (!Driver.isRegistered()) {
			Driver.register();
		}
		var pgds = new PGSimpleDataSource();
		pgds.setServerNames(new String[] { host });
		pgds.setPortNumbers(new int[] { port });
		pgds.setDatabaseName(database);
		pgds.setUser(user);
		pgds.setPassword(password);
		pgds.setReWriteBatchedInserts(true);
		var result = new HikariDataSource();
		result.setDataSource(pgds);
		result.setMaximumPoolSize(poolSize);
		return result;
	}

	/**
	 * Used for
	 * {@link TimedataTimescaleDb#getChannelIdsFromSchemaCache(Schema, String, Set)}.
	 */
	protected static class TemporaryChannelRecord {
		public final ChannelAddress address;
		public final ChannelRecord meta;

		protected TemporaryChannelRecord(ChannelAddress address, ChannelRecord meta) {
			this.address = address;
			this.meta = meta;
		}
	}

	/**
	 * Gets the database Channel-IDs for the given Channel-Addresses from the Schema
	 * Cache.
	 * 
	 * @param schema           the {@link Schema}
	 * @param edgeId           the Edge-ID
	 * @param channelAddresses a {@link Set} of Channel-Addresses
	 * @return a map of {@link Type}s to {@link Priority}s to Channel-IDs to
	 *         Channel-Addresses
	 */
	public static Map<Type, Map<Priority, Map<Integer, String>>> querySchemaCache(Schema schema, String edgeId,
			Set<String> channelAddresses) {
		var result = new EnumMap<Type, Map<Priority, Map<Integer, String>>>(Type.class);
		var missingChannels = new ArrayList<String>();
		for (var channelAddress : channelAddresses) {
			var meta = schema.getChannelFromCache(edgeId, channelAddress);
			if (meta == null) {
				missingChannels.add(channelAddress);
				continue;
			}
			var priorityMap = result.computeIfAbsent(meta.type,
					(m) -> new EnumMap<Priority, Map<Integer, String>>(Priority.class));
			var ids = priorityMap.computeIfAbsent(meta.priority, t -> new HashMap<>());
			ids.put(meta.id, channelAddress);
		}

		if (!missingChannels.isEmpty()) {
			// may happen if the edge never wrote to this channel
			LOG.warn("Missing Cache for [" + edgeId + "]: " + String.join(", ", missingChannels));
		}
		return result;
	}

	/**
	 * Prefills a Result-Map with JsonNull values for every
	 * timestamp/ChannelAddress.
	 * 
	 * @param fromDate   the From-Date
	 * @param toDate     the To-Date
	 * @param channels   the Channels
	 * @param resolution the {@link Resolution}
	 * @return a prefilled result-map
	 */
	public static TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> prepareDataMap(ZonedDateTime fromDate,
			ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution) {
		TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> result = new TreeMap<>();

		// Prepare a map from ChannelAddress to JsonNull
		var channelMap = channels.stream().collect(Collectors.<ChannelAddress, ChannelAddress, JsonElement, //
				SortedMap<ChannelAddress, JsonElement>>toMap(//
						Function.identity(), // Key
						c -> JsonNull.INSTANCE, // Value
						(key1, key2) -> key1, // duplicate KEY resolver - can never happen
						TreeMap<ChannelAddress, JsonElement>::new)); // Create a TreeMap

		var timestamp = fromDate;
		while (timestamp.isBefore(toDate)) {
			result.put(timestamp, new TreeMap<>(channelMap) /* individual copy for each timestamp */);
			timestamp = timestamp.plus(resolution.getValue(), resolution.getUnit());
		}

		return result;
	}

	/**
	 * Prefills a Result-Map with JsonNull values for every
	 * timestamp/ChannelAddress.
	 * 
	 * @param fromDate the From-Date
	 * @param toDate   the To-Date
	 * @param channels the Channels
	 * @return a prefilled result-map
	 */
	public static SortedMap<ChannelAddress, JsonElement> prepareEnergyMap(ZonedDateTime fromDate, ZonedDateTime toDate,
			Set<ChannelAddress> channels) {
		// Prepare a map from ChannelAddress to JsonNull
		return channels.stream().collect(Collectors.<ChannelAddress, ChannelAddress, JsonElement, //
				SortedMap<ChannelAddress, JsonElement>>toMap(//
						Function.identity(), // Key
						c -> JsonNull.INSTANCE, // Value
						(key1, key2) -> key1, // duplicate KEY resolver - can never happen
						TreeMap<ChannelAddress, JsonElement>::new)); // Create a TreeMap
	}

	/**
	 * Converts a Resolution to an SQL interval.
	 * 
	 * @param resolution the {@link Resolution}
	 * @return a SQL interval string
	 */
	public static String toSqlInterval(Resolution resolution) {
		var unit = resolution.getUnit();
		return switch (unit) {
		
		case YEARS, MONTHS, WEEKS, DAYS, HALF_DAYS, HOURS, MINUTES, SECONDS  -> 
			resolution.getValue() + " " + unit.toString();
			
		case MILLIS, MICROS, NANOS,FOREVER, ERAS, MILLENNIA, CENTURIES, DECADES ->
		 	throw new IllegalArgumentException("Resolution " + resolution.getUnit() + " is not supported");		
			
		};
		
	}

}
