package io.openems.backend.timedata.timescaledb.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.timedata.timescaledb.internal.write.Point;

public class Schema {

	public static class ChannelRecord {
		public final int id;
		public final Type type;
		public final ZonedDateTime availableSince;

		public ChannelRecord(int id, Type type, ZonedDateTime availableSince) {
			this.id = id;
			this.type = type;
			this.availableSince = availableSince;
		}
	}

	private static class Cache {

		/**
		 * Queries the existing data type mappings.
		 * 
		 * @param stmnt {@link Statement}
		 * @return the {@link Cache}
		 * @throws SQLException on error
		 */
		private static Cache fromDatabase(Statement stmnt) throws SQLException {
			var rs = stmnt.executeQuery("" //
					+ "SELECT" //
					+ "    edge.name AS edge," //
					+ "    channel.address AS channelAddress," //
					+ "    edge_channel.id AS channelId," //
					+ "    edge_channel.type AS type," //
					+ "    edge_channel.available_since as available_since" //
					+ " FROM \"edge_channel\"" //
					+ "  INNER JOIN \"edge\"" //
					+ "   ON edge_channel.edge_id = edge.id" //
					+ "  INNER JOIN channel" //
					+ "   ON edge_channel.channel_id = channel.id"); //
			var cache = new Cache();
			while (rs.next()) {
				var edge = rs.getString("edge");
				var channelAddress = rs.getString("channelAddress");
				var channelId = rs.getInt("channelId");
				var channelType = rs.getInt("type");
				var availableSince = rs.getObject("available_since", OffsetDateTime.class).toZonedDateTime();
				cache.add(edge, channelAddress, channelId, channelType, availableSince);
			}
			return cache;
		}

		private final Map<String /* Edge-ID */, //
				Map<String /* Channel-Address */, //
						ChannelRecord /* Meta-Info for Channel */>> channels = new HashMap<String, Map<String, ChannelRecord>>();

		protected Cache() {
		}

		/**
		 * Adds a {@link ChannelRecord} to the given Cache Map.
		 * 
		 * @param edgeName       the Edge-Name
		 * @param channelAddress the Channel-Address
		 * @param channelId      the Channel-Database-ID
		 * @param typeId         the Type Database-ID
		 * @param availableSince the timestamp since the value is available in
		 *                       TimescaleDB
		 * @return the {@link ChannelRecord}
		 */
		public synchronized ChannelRecord add(String edgeName, String channelAddress, int channelId, int typeId,
				ZonedDateTime availableSince) {
			var type = Type.fromId(typeId);
			var edge = this.channels.computeIfAbsent(edgeName, //
					(k) -> new HashMap<String, ChannelRecord>());
			var channel = edge.computeIfAbsent(channelAddress, //
					(k) -> new ChannelRecord(channelId, type, availableSince));
			return channel;
		}

		/**
		 * Gets the {@link ChannelRecord} from local Cache.
		 * 
		 * @param edgeId         the Edge-ID
		 * @param channelAddress the Channel-Address
		 * @return the {@link ChannelRecord} with a database ID for table 'channel',
		 *         null if there is no entry yet
		 */
		public ChannelRecord get(String edgeId, String channelAddress) {
			var edge = this.channels.get(edgeId);
			if (edge == null) {
				return null;
			}
			var channel = edge.get(channelAddress);
			if (channel == null) {
				return null;
			}
			return channel;
		}
	}

	/**
	 * Initialize the database Schema and the Channels Cache.
	 * 
	 * @param dataSource a {@link HikariDataSource}
	 * @return the {@link Schema}
	 * @throws SQLException on error
	 */
	public static Schema initialize(HikariDataSource dataSource) throws SQLException {
		try (var con = dataSource.getConnection()) {
			var stmnt = con.createStatement();
			var cache = Cache.fromDatabase(stmnt);
			return new Schema(cache);
		}
	}

	private final Cache cache;

	private Schema(Cache cache) {
		this.cache = cache;
	}

	/**
	 * Gets the Channel for the given {@link Point}. Adds it if it was not existing
	 * before.
	 * 
	 * @param con            a database {@link Connection}, in case the entry needs
	 *                       to be added
	 * @param edgeId         the Edge-ID
	 * @param channelAddress the Channel-Address
	 * @param value          the {@link JsonElement} value
	 * @return the {@link ChannelRecord}; or null if not in Cache and type cannot be
	 *         detected
	 * @throws SQLException on error while adding
	 */
	public ChannelRecord getChannel(Connection con, String edgeId, String channelAddress, JsonElement value)
			throws SQLException {
		// Cache-Lookup
		var result = this.getChannelFromCache(edgeId, channelAddress);
		if (result != null) {
			return result;
		}
		// Missing in Cache -> add to database
		var type = Type.detect(value);
		if (type == null) {
			// unable to detect
			return null;
		}
		// Get or Create Channel-ID
		return this.getOrCreateEdgeChannel(con, edgeId, channelAddress, value, type);
	}

	/**
	 * Gets the {@link ChannelRecord} from local Cache.
	 * 
	 * @param edgeId         the Edge-ID
	 * @param channelAddress the Channel-Address
	 * @return the {@link ChannelRecord} with a database ID for table 'channel',
	 *         null if there is no entry yet
	 */
	public ChannelRecord getChannelFromCache(String edgeId, String channelAddress) {
		return this.cache.get(edgeId, channelAddress);
	}

	/**
	 * Gets or creates the {@link ChannelRecord} in the database and adds it to the
	 * local Cache.
	 * 
	 * @param con            the {@link Connection}
	 * @param edgeId         the Edge-ID
	 * @param channelAddress the Channel-Address
	 * @param value          the {@link JsonElement} value
	 * @param type           the {@link Type}
	 * @return the {@link ChannelRecord} with a database ID for table 'channel'
	 * @throws SQLException on error
	 */
	private ChannelRecord getOrCreateEdgeChannel(Connection con, String edgeId, String channelAddress,
			JsonElement value, Type type) throws SQLException {
		var pst = con.prepareStatement("" //
				+ "SELECT _channel_id, _channel_type, _available_since " //
				+ "FROM openems_get_or_create_edge_channel_id(?, ?, ?);");
		pst.setString(1, edgeId);
		pst.setString(2, channelAddress);
		pst.setInt(3, type.id);
		var rs = pst.executeQuery();
		rs.next();
		var channelId = rs.getInt(1);
		var channelTypeId = rs.getInt(2);
		final ZonedDateTime availableSince;
		var availableSinceRaw = rs.getObject(3, OffsetDateTime.class);
		if (availableSinceRaw != null) {
			availableSince = availableSinceRaw.toZonedDateTime();
		} else {
			availableSince = ZonedDateTime.now(); // consider setting this to null with @Nullable annotation
		}
		return this.cache.add(edgeId, channelAddress, channelId, channelTypeId, availableSince);
	}

}
