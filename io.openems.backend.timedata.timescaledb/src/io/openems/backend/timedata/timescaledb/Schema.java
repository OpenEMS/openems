package io.openems.backend.timedata.timescaledb;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.zaxxer.hikari.HikariDataSource;

public class Schema {

	public static class ChannelMeta {
		public final int id;
		public final Type type;

		public ChannelMeta(int id, Type type) {
			this.id = id;
			this.type = type;
		}
	}

	private final Map<String /* Edge-ID */, //
			Map<String /* Component-ID */, //
					Map<String /* Channel-ID */, //
							ChannelMeta /* Meta-Info for Channel */>>> channels;

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
			createViewMeta(stmnt);

			// Create PL/SQL functions
			createFunctionGetOrCreateChannelId(stmnt);
			createFunctionGetOrCreateComponentId(stmnt);
			createFunctionGetOrCreateEdgeId(stmnt);

			// Create raw and aggregated data tables
			for (var type : Type.values()) {
				createHypertable(stmnt, type);
				createContinuousAggregate5m(stmnt, type);
			}

			var channels = queryTypes(stmnt);
			return new Schema(channels);
		}
	}

	private Schema(Map<String, Map<String, Map<String, ChannelMeta>>> channels) {
		this.channels = channels;
	}

	/**
	 * Gets the Channel for the given {@link Point}. Adds it if it was not existing
	 * before.
	 * 
	 * @param point the {@link Point}
	 * @param con   a database {@link Connection}, in case the entry needs to be
	 *              added
	 * @return the {@link ChannelMeta}; or null if not in Cache and type cannot be
	 *         detected
	 * @throws SQLException on error while adding
	 */
	public ChannelMeta getChannel(Point point, Connection con) throws SQLException {
		// Cache-Lookup
		var result = this.getChannelFromCache(point.edgeId, point.channelAddress.getComponentId(),
				point.channelAddress.getChannelId());
		if (result != null) {
			return result;
		}
		// Missing in Cache -> add to database
		var type = Type.detect(point.value);
		if (type == null) {
			// unable to detect
			return null;
		}
		// Get or Create Channel-ID
		return this.getOrCreateChannel(point, type, con);
	}

	/**
	 * Gets the {@link ChannelMeta} from local Cache.
	 * 
	 * @param edgeId      the Edge-ID
	 * @param componentId the Component-ID
	 * @param channelId   the Channel-ID
	 * @return the database ID for table 'channel', null if there is no entry yet
	 */
	public ChannelMeta getChannelFromCache(String edgeId, String componentId, String channelId) {
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

	/**
	 * Gets or creates the {@link ChannelMeta} in the database and adds it to the
	 * local Cache.
	 * 
	 * @param point the {@link Point}
	 * @param type  the {@link Type}
	 * @param con   the {@link Connection}
	 * @return the database ID for table 'channel', null if there is no entry yet
	 */
	private ChannelMeta getOrCreateChannel(Point point, Type type, Connection con) throws SQLException {
		var pst = con.prepareStatement("" //
				+ "SELECT _channel_id, _channel_type " //
				+ "FROM openems_get_or_create_channel_id(?, ?, ?, ?);");
		pst.setString(1, point.edgeId);
		pst.setString(2, point.channelAddress.getComponentId());
		pst.setString(3, point.channelAddress.getChannelId());
		pst.setInt(4, type.id);
		var resultSet = pst.executeQuery();
		resultSet.next();
		var channelId = resultSet.getInt("_channel_id");
		var channelTypeId = resultSet.getInt("_channel_type");
		return addToCache(this.channels, point.edgeId, point.channelAddress.getComponentId(),
				point.channelAddress.getChannelId(), channelId, channelTypeId);
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
				+ "	   edge_id INTEGER NOT NULL REFERENCES edge," //
				+ "    name TEXT NOT NULL," //
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
	 * Creates the 'meta' view.
	 * 
	 * <p>
	 * Convenient view on Edge, Component and Channel tables.
	 * 
	 * @param stmnt {@link Statement}
	 * @throws SQLException on error
	 */
	private static void createViewMeta(Statement stmnt) throws SQLException {
		stmnt.execute("" //
				+ "CREATE OR REPLACE VIEW \"meta\" AS " //
				+ "SELECT" //
				+ "    channel.id AS channel_id," //
				+ "    edge.name AS edge," //
				+ "    component.name AS component," //
				+ "    channel.name AS channel," //
				+ "    CASE " //
				+ Stream.of(Type.values()) //
						.map(t -> "WHEN channel.type = " + t.id + " THEN '" + t.sqlDataType + "'") //
						.collect(Collectors.joining(" ")) //
				+ "    END AS type " //
				+ "FROM edge " //
				+ "INNER JOIN component " //
				+ "ON edge.id = component.edge_id " //
				+ "INNER JOIN channel " //
				+ "ON component.id = channel.component_id;");
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
				+ "    time TIMESTAMPTZ NOT NULL," //
				+ "    channel_id INTEGER NOT NULL," //
				+ "    value " + type.sqlDataType + " NULL" //
				+ ");");
		stmnt.execute("" //
				+ "SELECT create_hypertable('" + type.tableRaw + "', 'time');");
		stmnt.execute("" //
				+ "CREATE INDEX ix_" + type.tableRaw + "_channel_time " //
				+ "ON " + type.tableRaw + " (time, channel_id DESC);");
		stmnt.execute("" //
				+ "SELECT add_retention_policy('" + type.tableRaw + "', INTERVAL '31 days');");
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
		stmnt.execute("" //
				+ "SELECT add_continuous_aggregate_policy('" + type.tableAggregate5m + "'," //
				+ "     start_offset => INTERVAL '30 days'," //
				+ "     end_offset => INTERVAL '1 hours'," //
				+ "     schedule_interval => INTERVAL '6 hours'" //
				+ ");");
	}

	/**
	 * Queries the existing data type mappings.
	 * 
	 * @param stmnt {@link Statement}
	 * @return the Cache
	 * @throws SQLException on error
	 */
	private static Map<String, Map<String, Map<String, ChannelMeta>>> queryTypes(Statement stmnt) throws SQLException {
		var channels = new HashMap<String, Map<String, Map<String, ChannelMeta>>>();
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
		while (resultSet.next()) {
			var edge = resultSet.getString("edge");
			var component = resultSet.getString("component");
			var channel = resultSet.getString("channel");
			var channelId = resultSet.getInt("channelId");
			var channelType = resultSet.getInt("type");
			addToCache(channels, edge, component, channel, channelId, channelType);
		}
		return channels;
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

	/**
	 * Adds a {@link ChannelMeta} to the given Cache Map.
	 * 
	 * @param cache         the Cache
	 * @param edgeName      the Edge-Name
	 * @param componentName the Component-Name
	 * @param channelName   the Channel-Name
	 * @param channelId     the Channel-Database-ID
	 * @param typeId        the Type Database-ID
	 * @return the {@link ChannelMeta}
	 */
	private static ChannelMeta addToCache(Map<String, Map<String, Map<String, ChannelMeta>>> cache, String edgeName,
			String componentName, String channelName, int channelId, int typeId) {
		var type = Type.fromId(typeId);
		var edge = cache.computeIfAbsent(edgeName, //
				(k) -> new HashMap<String, Map<String, ChannelMeta>>());
		var component = edge.computeIfAbsent(componentName, //
				(k) -> new HashMap<String, ChannelMeta>());
		var channel = component.computeIfAbsent(channelName, //
				(k) -> new ChannelMeta(channelId, type));
		return channel;
	}

}
