package io.openems.backend.timedata.timescaledb.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.timedata.timescaledb.internal.write.Point;
import io.openems.common.types.ChannelAddress;

public class Schema {

	public static class ChannelRecord {
		public final int id;
		public final Type type;

		public ChannelRecord(int id, Type type) {
			this.id = id;
			this.type = type;
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
			var resultSet = stmnt.executeQuery("" //
					+ "SELECT" //
					+ "    edge.name AS edge," //
					+ "    component.name AS component," //
					+ "    channel.name AS channel," //
					+ "    channel.id AS channelId," //
					+ "    channel.type AS type " //
					+ "FROM \"edge\" " //
					+ "INNER JOIN \"component\" " //
					+ "ON edge.id = component.edge_id" //
					+ "    INNER JOIN \"channel\"" //
					+ "    ON component.id = channel.component_id;"); //
			var cache = new Cache();
			while (resultSet.next()) {
				var edge = resultSet.getString("edge");
				var component = resultSet.getString("component");
				var channel = resultSet.getString("channel");
				var channelId = resultSet.getInt("channelId");
				var channelType = resultSet.getInt("type");
				cache.add(edge, component, channel, channelId, channelType);
			}
			return cache;
		}

		private final Map<String /* Edge-ID */, //
				Map<String /* Component-ID */, //
						Map<String /* Channel-ID */, //
								ChannelRecord /* Meta-Info for Channel */>>> channels = new HashMap<String, Map<String, Map<String, ChannelRecord>>>();

		protected Cache() {
		}

		/**
		 * Adds a {@link ChannelRecord} to the given Cache Map.
		 * 
		 * @param edgeName      the Edge-Name
		 * @param componentName the Component-Name
		 * @param channelName   the Channel-Name
		 * @param channelId     the Channel-Database-ID
		 * @param typeId        the Type Database-ID
		 * @return the {@link ChannelRecord}
		 */
		public synchronized ChannelRecord add(String edgeName, String componentName, String channelName, int channelId,
				int typeId) {
			var type = Type.fromId(typeId);
			var edge = this.channels.computeIfAbsent(edgeName, //
					(k) -> new HashMap<String, Map<String, ChannelRecord>>());
			var component = edge.computeIfAbsent(componentName, //
					(k) -> new HashMap<String, ChannelRecord>());
			var channel = component.computeIfAbsent(channelName, //
					(k) -> new ChannelRecord(channelId, type));
			return channel;
		}

		/**
		 * Gets the {@link ChannelRecord} from local Cache.
		 * 
		 * @param edgeId      the Edge-ID
		 * @param componentId the Component-ID
		 * @param channelId   the Channel-ID
		 * @return the {@link ChannelRecord} with a database ID for table 'channel',
		 *         null if there is no entry yet
		 */
		public ChannelRecord get(String edgeId, String componentId, String channelId) {
			var edge = this.channels.get(edgeId);
			if (edge == null) {
				return null;
			}
			var component = edge.get(componentId);
			if (component == null) {
				return null;
			}
			var channel = component.get(channelId);
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

			// Create tables
			createTableEdge(stmnt);
			createTableComponent(stmnt);
			createTableChannel(stmnt);

			// Create PL/SQL functions
			createFunctionGetOrCreateChannelId(stmnt);
			createFunctionGetOrCreateComponentId(stmnt);
			createFunctionGetOrCreateEdgeId(stmnt);

			// Create raw and aggregated data tables
			for (var type : Type.values()) {
				createHypertable(stmnt, type);
				createContinuousAggregate5m(stmnt, type);
			}

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
	 * @param channelAddress the {@link ChannelAddress}
	 * @param value          the {@link JsonElement} value
	 * @return the {@link ChannelRecord}; or null if not in Cache and type cannot be
	 *         detected
	 * @throws SQLException on error while adding
	 */
	public ChannelRecord getChannel(Connection con, String edgeId, ChannelAddress channelAddress, JsonElement value)
			throws SQLException {
		// Cache-Lookup
		var result = this.getChannelFromCache(edgeId, channelAddress.getComponentId(), channelAddress.getChannelId());
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
		return this.getOrCreateChannel(con, edgeId, channelAddress, value, type);
	}

	/**
	 * Gets the {@link ChannelRecord} from local Cache.
	 * 
	 * @param edgeId      the Edge-ID
	 * @param componentId the Component-ID
	 * @param channelId   the Channel-ID
	 * @return the {@link ChannelRecord} with a database ID for table 'channel',
	 *         null if there is no entry yet
	 */
	public ChannelRecord getChannelFromCache(String edgeId, String componentId, String channelId) {
		return this.cache.get(edgeId, componentId, channelId);
	}

	/**
	 * Gets or creates the {@link ChannelRecord} in the database and adds it to the
	 * local Cache.
	 * 
	 * @param con            the {@link Connection}
	 * @param edgeId         the Edge-ID
	 * @param channelAddress the {@link ChannelAddress}
	 * @param value          the {@link JsonElement} value
	 * @param type           the {@link Type}
	 * @return the {@link ChannelRecord} with a database ID for table 'channel'
	 */
	private ChannelRecord getOrCreateChannel(Connection con, String edgeId, ChannelAddress channelAddress,
			JsonElement value, Type type) throws SQLException {
		var pst = con.prepareStatement("" //
				+ "SELECT _channel_id, _channel_type " //
				+ "FROM openems_get_or_create_channel_id(?, ?, ?, ?);");
		pst.setString(1, edgeId);
		pst.setString(2, channelAddress.getComponentId());
		pst.setString(3, channelAddress.getChannelId());
		pst.setInt(4, type.id);
		var resultSet = pst.executeQuery();
		resultSet.next();
		var channelId = resultSet.getInt("_channel_id");
		var channelTypeId = resultSet.getInt("_channel_type");
		return this.cache.add(edgeId, channelAddress.getComponentId(), channelAddress.getChannelId(), channelId,
				channelTypeId);
	}

	/**
	 * Creates the 'edge' table.
	 * 
	 * <p>
	 * Stores OpenEMS Edge devices by their Edge-IDs (like 'edge0') and generates a
	 * unique ID.
	 * 
	 * @param stmnt {@link Statement}
	 * @throws SQLException on error
	 */
	private static void createTableEdge(Statement stmnt) throws SQLException {
		stmnt.executeUpdate("" //
				+ "CREATE TABLE IF NOT EXISTS \"edge\" (" //
				+ "    id SERIAL PRIMARY KEY," //
				+ "    name TEXT NOT NULL," //
				+ "    UNIQUE(name)" //
				+ ");");
	}

	/**
	 * Creates the 'component' table.
	 * 
	 * <p>
	 * Stores OpenEMS Components of OpenEMS Edge devices by their Component-IDs
	 * (like '_sum' or 'ess0'), generates a unique ID and links to the 'edge' table.
	 * 
	 * @param stmnt {@link Statement}
	 * @throws SQLException on error
	 */
	private static void createTableComponent(Statement stmnt) throws SQLException {
		stmnt.execute("" //
				+ "CREATE TABLE IF NOT EXISTS \"component\" (" //
				+ "    id SERIAL PRIMARY KEY," //
				+ "    edge_id INTEGER NOT NULL REFERENCES edge," //
				+ "    name TEXT NOT NULL," //
				+ "    available_since TIMESTAMPTZ," //
				+ "    UNIQUE(edge_id, name)" //
				+ ");");
	}

	/**
	 * Creates the 'channel' table.
	 * 
	 * <p>
	 * Stores Channels of OpenEMS Components of OpenEMS Edge devices by their
	 * Channel-IDs (like 'EssSoc', 'ActivePower' or 'State'), generates a unique ID
	 * and links to the 'component' table.
	 * 
	 * @param stmnt {@link Statement}
	 * @throws SQLException on error
	 */
	private static void createTableChannel(Statement stmnt) throws SQLException {
		stmnt.execute("" //
				+ "CREATE TABLE IF NOT EXISTS \"channel\" (" //
				+ "    id SERIAL PRIMARY KEY," //
				+ "	   component_id INTEGER NOT NULL REFERENCES component," //
				+ "    name TEXT NOT NULL," //
				+ "    type INTEGER NOT NULL," //
				+ "    UNIQUE(component_id, name)" //
				+ ");");
	}

	/**
	 * Creates the 'openems_get_or_create_channel_id' function.
	 * 
	 * <p>
	 * <ul>
	 * <li>If an entry in 'channel' table exists for the given combination of
	 * Edge-ID, Component-ID and Channel-ID -> return the channel_id
	 * <li>If no entry exists -> create a new one and return the channel_id
	 * </ul>
	 * 
	 * @param stmnt {@link Statement}
	 * @throws SQLException on error
	 */
	private static void createFunctionGetOrCreateChannelId(Statement stmnt) throws SQLException {
		stmnt.execute("" //
				+ "CREATE OR REPLACE FUNCTION openems_get_or_create_channel_id(_edge text, _component text, _channel text, _type int, OUT _channel_id int, OUT _channel_type int)" //
				+ "  LANGUAGE plpgsql AS " //
				+ "$$ " //
				+ "BEGIN" //
				+ "    LOOP" //
				+ "        SELECT channel.id, channel.type" //
				+ "        FROM edge" //
				+ "        LEFT JOIN component" //
				+ "        ON edge.id = component.edge_id" //
				+ "        LEFT JOIN channel" //
				+ "        ON component.id = channel.component_id" //
				+ "        WHERE edge.name = _edge AND component.name = _component AND channel.name = _channel" //
				+ "        INTO _channel_id, _channel_type;" //
				+ "" //
				+ "        EXIT WHEN FOUND;" //
				+ "" //
				+ "        INSERT INTO channel (component_id, name, type)" //
				+ "        VALUES ((SELECT _component_id FROM openems_get_or_create_component_id(_edge, _component)), _channel, _type)" //
				+ "        ON CONFLICT DO NOTHING" //
				+ "        RETURNING id, type" //
				+ "        INTO _channel_id, _channel_type;" //
				+ "" //
				+ "        EXIT WHEN FOUND;" //
				+ "    END LOOP;" //
				+ "END " //
				+ "$$;");
	}

	/**
	 * Creates the 'openems_get_or_create_component_id' function.
	 * 
	 * <p>
	 * <ul>
	 * <li>If an entry in 'component' table exists for the given combination of
	 * Edge-ID and Component-ID -> return the component_id
	 * <li>If no entry exists -> create a new one and return the component_id
	 * </ul>
	 * 
	 * @param stmnt {@link Statement}
	 * @throws SQLException on error
	 */
	private static void createFunctionGetOrCreateComponentId(Statement stmnt) throws SQLException {
		stmnt.execute("" //
				+ "CREATE OR REPLACE FUNCTION openems_get_or_create_component_id(_edge text, _component text, OUT _component_id int)" //
				+ "    LANGUAGE plpgsql AS " //
				+ "$$" //
				+ "BEGIN" //
				+ "    LOOP" //
				+ "        SELECT component.id" //
				+ "        FROM edge" //
				+ "        LEFT JOIN component" //
				+ "        ON edge.id = component.edge_id" //
				+ "        WHERE edge.name = _edge AND component.name = _component" //
				+ "        INTO _component_id;" //
				+ "" //
				+ "        EXIT WHEN FOUND;" //
				+ "" //
				+ "        INSERT INTO component (edge_id, name)" //
				+ "        VALUES ((SELECT _edge_id FROM openems_get_or_create_edge_id(_edge)), _component)" //
				+ "        ON CONFLICT DO NOTHING" //
				+ "        RETURNING id" //
				+ "        INTO _component_id;" //
				+ "" //
				+ "        EXIT WHEN FOUND;" //
				+ "    END LOOP;" //
				+ "END " //
				+ "$$;");
	}

	/**
	 * Creates the 'openems_get_or_create_edge_id' function.
	 * 
	 * <p>
	 * <ul>
	 * <li>If an entry in 'edge' table exists for the given Edge-ID -> return the
	 * edge_id
	 * <li>If no entry exists -> create a new one and return the edge_id
	 * </ul>
	 * 
	 * @param stmnt {@link Statement}
	 * @throws SQLException on error
	 */
	private static void createFunctionGetOrCreateEdgeId(Statement stmnt) throws SQLException {
		stmnt.execute("" //
				+ "CREATE OR REPLACE FUNCTION openems_get_or_create_edge_id(_edge text, OUT _edge_id int)" //
				+ "    LANGUAGE plpgsql AS " //
				+ "$$ " //
				+ "BEGIN" //
				+ "    LOOP" //
				+ "        SELECT id" //
				+ "        FROM edge" //
				+ "        WHERE edge.name = _edge" //
				+ "        INTO _edge_id;" //
				+ "" //
				+ "        EXIT WHEN FOUND;" //
				+ "" //
				+ "        INSERT INTO edge (name)" //
				+ "        VALUES (_edge)" //
				+ "        ON CONFLICT DO NOTHING" //
				+ "        RETURNING id" //
				+ "        INTO _edge_id;" //
				+ "" //
				+ "        EXIT WHEN FOUND;" //
				+ "    END LOOP;" //
				+ "END " //
				+ "$$;");
	}

	/**
	 * Creates the hypertable for raw data and adds a retention policy.
	 * 
	 * <p>
	 * Stores the raw Channel data.
	 * 
	 * @param stmnt {@link Statement}
	 * @param type  the {@link Type}
	 * @throws SQLException on error
	 */
	private static void createHypertable(Statement stmnt, Type type) throws SQLException {
		// Do not continue if table already exists
		if (doesTableExist(stmnt, type.tableRaw)) {
			return;
		}

		stmnt.execute("" //
				+ "CREATE TABLE IF NOT EXISTS \"" + type.tableRaw + "\" (" //
				+ "    time TIMESTAMPTZ(3) NOT NULL," //
				+ "    channel_id INTEGER NOT NULL," //
				+ "    value " + type.sqlDataType + " NULL" //
				+ ");");
		stmnt.execute("" //
				+ "SELECT create_hypertable('" + type.tableRaw
				// TODO check intervals
				+ "', 'time', chunk_time_interval => INTERVAL '1 hour');");
		stmnt.execute("" //
				+ "CREATE INDEX ix_" + type.tableRaw + "_time_channel " //
				+ "ON " + type.tableRaw + " (time DESC, channel_id ASC);");

		// Compression. See
		// https://docs.timescale.com/timescaledb/latest/how-to-guides/compression/about-compression/
		stmnt.execute("" //
				+ "ALTER TABLE " + type.tableRaw + " SET (" //
				+ "    timescaledb.compress," //
				+ "    timescaledb.compress_segmentby = 'channel_id'" //
				+ ");");
		// TODO check interval parameter
		stmnt.execute("" //
				+ "SELECT add_compression_policy('" + type.tableRaw + "', INTERVAL '2 days');");

		// TODO check retention interval vs continuous query
		// stmnt.execute("" //
		// + "SELECT add_retention_policy('" + type.tableRaw + "', INTERVAL '31
		// days');");
	}

	/**
	 * Creates the continuous aggregate and adds an aggregate policy.
	 * 
	 * <p>
	 * Stores the Channel data, aggregated for 5 minutes.
	 * 
	 * @param stmnt {@link Statement}
	 * @param type  the {@link Type}
	 * @throws SQLException on error
	 */
	private static void createContinuousAggregate5m(Statement stmnt, Type type) throws SQLException {
		// Do not continue if view already exists
		if (doesTableExist(stmnt, type.tableAggregate5m)) {
			return;
		}

		stmnt.execute("" //
				+ "CREATE MATERIALIZED VIEW \"" + type.tableAggregate5m + "\" (" //
				+ "    time," //
				+ "    channel_id," //
				+ Stream.of(type.aggregateFunctions) //
						.map(s -> "\"" + s + "\"") //
						.collect(Collectors.joining(", "))
				+ ")" //
				+ "WITH (timescaledb.continuous) AS" //
				+ "	   SELECT " //
				+ "        time_bucket('5 minutes', time) AS time," //
				+ "        channel_id," //
				+ Stream.of(type.aggregateFunctions) //
						.map(s -> s + "(\"value\")") //
						.collect(Collectors.joining(", "))
				+ "    FROM \"" + type.tableRaw + "\"" //
				+ "	   GROUP BY (1,2);");

		// TODO READ Hypertable for materialized view:
		// SELECT view_name, format('%I.%I', materialization_hypertable_schema,
		// materialization_hypertable_name) AS materialization_hypertable
		// FROM timescaledb_information.continuous_aggregates where
		// view_name='data_integer_5m';

		// TODO Add reorder policy
		// SELECT
		// add_reorder_policy('_timescaledb_internal._materialized_hypertable_12',
		// '_materialized_hypertable_12_channel_id_time_idx');

		// TODO: konfiguriere chunk size
		// SELECT set_chunk_time_interval('data_integer_raw', INTERVAL '1 hour');

		stmnt.execute("" //
				+ "SELECT add_continuous_aggregate_policy('" + type.tableAggregate5m + "'," //
				// TODO remove start_offset temporarily to allow backfill
				+ "     start_offset => INTERVAL '30 days'," //
				+ "     end_offset => INTERVAL '1 hours'," //
				+ "     schedule_interval => INTERVAL '6 hours'" //
				+ ");");

	}

	private static boolean doesTableExist(Statement stmnt, String tableName) throws SQLException {
		var resultSet = stmnt.executeQuery("" //
				+ "SELECT EXISTS (" //
				+ "   SELECT * FROM information_schema.tables " //
				+ "   WHERE table_schema = 'public'" //
				+ "     AND table_name = '" + tableName + "'" //
				+ ");");
		resultSet.next();
		return resultSet.getBoolean(1);
	}

}
