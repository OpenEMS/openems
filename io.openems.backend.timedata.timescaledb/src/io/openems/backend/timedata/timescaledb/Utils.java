package io.openems.backend.timedata.timescaledb;

import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.backend.timedata.timescaledb.Schema.ChannelMeta;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;

public class Utils {

	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private Utils() {

	}

	/**
	 * Used for
	 * {@link TimescaledbImpl#getChannelIdsFromSchemaCache(Schema, String, Set)}.
	 */
	protected static class TemporaryChannelRecord {
		public final ChannelAddress address;
		public final ChannelMeta meta;

		protected TemporaryChannelRecord(ChannelAddress address, ChannelMeta meta) {
			this.address = address;
			this.meta = meta;
		}
	}

	/**
	 * Gets the database Channel-IDs for the given ChannelAddresses from the Schema
	 * Cache.
	 * 
	 * @param schema           the {@link Schema}
	 * @param edgeId           the Edge-ID
	 * @param channelAddresses a {@link Set} of {@link ChannelAddress}es
	 * @return a map of {@link Type}s to Channel-IDs to {@link ChannelAddress}es
	 */
	public static Map<Type, Map<Integer, ChannelAddress>> querySchemaCache(Schema schema, String edgeId,
			Set<ChannelAddress> channelAddresses) {
		var result = new EnumMap<Type, Map<Integer, ChannelAddress>>(Type.class);
		for (var channelAddress : channelAddresses) {
			var meta = schema.getChannelFromCache(edgeId, channelAddress.getComponentId(),
					channelAddress.getChannelId());
			if (meta == null) {
				LOG.warn("Missing Cache for " + channelAddress);
				continue;
			}
			var ids = result.computeIfAbsent(meta.type, (m) -> new HashMap<Integer, ChannelAddress>());
			ids.put(meta.id, channelAddress);
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
		switch (unit) {
		case FOREVER:
		case ERAS:
		case MILLENNIA:
		case CENTURIES:
		case DECADES:
			break;
		case YEARS:
		case MONTHS:
		case WEEKS:
		case DAYS:
		case HALF_DAYS:
		case HOURS:
		case MINUTES:
		case SECONDS:
			return resolution.getValue() + " " + unit.toString();
		case MILLIS:
		case MICROS:
		case NANOS:
			break;
		}
		throw new IllegalArgumentException("Resolution " + resolution.getUnit() + " is not supported");
	}

}
