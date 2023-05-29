package io.openems.backend.timedata.timescaledb.internal;

public class SchemaGenerator {

	/**
	 * Main.
	 * 
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		var generator = new SchemaGenerator();

		final var sql = generator.generate();

		System.out.println(sql);
	}

	private final String generate() {
		var sb = new StringBuilder() //
				.append("/* Create static tables */\n\n") //
				.append(this.createEdgeTable()) //
				.append(this.createChannelTable()) //
				.append(this.createEdgeChannelTable()) //
				.append(this.createEdgeChannelIndex()) //
				.append("\n/* Create data tables */\n") //
				.append(this.createDataTables()) //
				.append("\n/* Create PL/pgSQL functions */\n\n") //
				.append(this.createFunctionGetOrCreateEdgeId()) //
				.append(this.createFunctionGetOrCreateChannelId()) //
				.append(this.createFunctionGetOrCreateEdgeChannelId()) //
		;
		return sb.toString();
	}

	private String createEdgeTable() {
		return """
				CREATE TABLE IF NOT EXISTS edge (
				  id SERIAL primary key,
				  name text NOT NULL,
				  UNIQUE(name)
				);
				""";
	}

	private String createChannelTable() {
		return "CREATE TABLE IF NOT EXISTS channel (\n" //
				+ "  id SERIAL primary key,\n" //
				+ "  address TEXT NOT NULL,\n" //
				+ "  UNIQUE(address)\n" //
				+ ");\n\n";
	}

	private String createEdgeChannelTable() {
		return "CREATE TABLE IF NOT EXISTS edge_channel (\n" //
				+ "  id SERIAL primary key,\n" //
				+ "  edge_id INTEGER NOT NULL REFERENCES edge,\n" //
				+ "  channel_id INTEGER NOT NULL REFERENCES channel,\n" //
				+ "  type INTEGER NOT NULL,\n" //
				+ "  available_since TIMESTAMPTZ,\n" //
				+ "  UNIQUE(edge_id, channel_id)\n" //
				+ ");\n\n";
	}

	private String createEdgeChannelIndex() {
		return "CREATE INDEX ix_edge_channel ON edge_channel (edge_id, channel_id);\n\n";
	}

	private String createDataTables() {
		var sb = new StringBuilder();
		for (var type : Type.values()) {
			sb.append("\n/*   Type: " + type + " */\n") //
					.append(this.createRawTable(type)) //
					.append(this.createHyperTable(type)) //
					.append(this.createRawIndex(type)) //
					// .append(this.alterCompression(type)) //
					// .append(this.addCompression(type)) //
					.append(this.createAggregateTable(type)) //
					.append(this.addContinuousAggregate(type)) //
			;
		}
		return sb.toString();
	}

	private String createRawTable(Type type) {
		return "CREATE TABLE IF NOT EXISTS " + type.rawTableName + " (\n" //
				+ "  time TIMESTAMPTZ(3) NOT NULL,\n" //
				+ "  edge_channel_id INTEGER NOT NULL,\n" //
				+ "  value " + type.sqlDataType + " NOT NULL\n" //
				+ ");\n\n";
	}

	private String createHyperTable(Type type) {
		return "SELECT create_hypertable('" + type.rawTableName + "',\n" //
				+ "  'time',\n" //
				+ "  chunk_time_interval => interval '1 hour',\n" //
				+ "  create_default_indexes => false);\n\n";
	}

	// NOTE: this index causes slow insert rates, leading to BACKPRESSURE
	private String createRawIndex(Type type) {
		return "CREATE INDEX ix_" + type.rawTableName + "_channel_time ON " //
				+ type.rawTableName + "(edge_channel_id ASC, time DESC);\n\n";
	}

	// private String alterCompression(Type type, Priority priority) {
	// return "ALTER TABLE " + type.getRawTableName(priority) + " SET (\n" //
	// + " timescaledb.compress,\n" //
	// + " timescaledb.compress_segmentby = 'edge_channel_id'\n" //
	// + ");\n\n";
	// }

	// private String addCompression(Type type, Priority priority) {
	// return "SELECT add_compression_policy('" + type.getRawTableName(priority) +
	// "', interval '2 days');\n\n";
	// }

	private String createAggregateTable(Type type) {
		return "CREATE MATERIALIZED VIEW " + type.aggregate5mTableName //
				+ "(time, edge_channel_id, value)\n" //
				+ " WITH (timescaledb.continuous) AS\n" //
				+ " SELECT time_bucket ('5 minutes', time) AS time,\n" //
				+ " edge_channel_id,\n" //
				+ " " + type.aggregateFunction + "(\"value\") as value\n" //
				+ " FROM " + type.rawTableName + "\n" //
				+ " GROUP BY (1, 2)\n" //
				+ "WITH NO DATA;\n\n";
	}

	// See
	// https://docs.timescale.com/timescaledb/latest/how-to-guides/continuous-aggregates/drop-data/#set-up-downsampling-and-data-retention
	private String addContinuousAggregate(Type type) {
		return "SELECT add_continuous_aggregate_policy('" + type.aggregate5mTableName + "',\n" //
				+ " start_offset => interval '12 hours',\n" // TODO switch to 30 days after
				+ " end_offset => interval '15 minutes',\n" //
				+ " schedule_interval => interval '10 minutes'\n" //
				+ ");\n" //

				+ "SELECT set_chunk_time_interval(\n" //
				+ " (\n" //
				+ "  SELECT format('%I.%I', materialization_hypertable_schema, materialization_hypertable_name) AS materialization_hypertable\n"
				+ "  FROM timescaledb_information.continuous_aggregates\n" //
				+ "  WHERE view_name LIKE '" + type.aggregate5mTableName + "'\n"
				//
				+ " ), INTERVAL '12 hours'\n" //
				+ ");\n" //

				+ "SELECT alter_job(\n" //
				+ " (\n" //
				+ "  SELECT job_id\n" //
				+ "  FROM timescaledb_information.jobs j\n" //
				+ "  INNER JOIN timescaledb_information.continuous_aggregates ca\n" //
				+ "  ON j.hypertable_schema = ca.materialization_hypertable_schema\n" //
				+ "  AND j.hypertable_name = ca.materialization_hypertable_name\n" //
				+ "  WHERE ca.view_name like '" + type.aggregate5mTableName + "'\n" //
				+ " ), next_start => now()\n" //
				+ ");\n\n";
	}

	private String createFunctionGetOrCreateEdgeId() {
		return """
				CREATE OR REPLACE FUNCTION openems_get_or_create_edge_id(
				  _edge text,
				  OUT _edge_id int
				) LANGUAGE plpgsql AS
				$$
				BEGIN
				  LOOP
				    SELECT id
				    FROM edge
				    WHERE edge.name = _edge
				    INTO _edge_id;

				    EXIT WHEN FOUND;

				    INSERT INTO edge (name)
				    VALUES (_edge)
				    ON CONFLICT DO NOTHING
				    RETURNING id
				    INTO _edge_id;

				    EXIT WHEN FOUND;
				  END LOOP;
				END;
				$$;
				""";
	}

	private String createFunctionGetOrCreateChannelId() {
		return "CREATE OR REPLACE FUNCTION openems_get_or_create_channel_id(\n" //
				+ "  _channelAddress text,\n" //
				+ "  OUT _channel_id int\n" //
				+ ") LANGUAGE plpgsql AS \n" //
				+ "$$ \n" //
				+ "BEGIN\n" //
				+ "  LOOP\n" //
				+ "    SELECT id\n" //
				+ "    FROM channel\n" //
				+ "    WHERE address = _channelAddress\n" //
				+ "    INTO _channel_id;\n" //
				+ "\n" //
				+ "    EXIT WHEN FOUND;\n" //
				+ "\n" //
				+ "    INSERT INTO channel (address)\n" //
				+ "    VALUES (_channelAddress)\n" //
				+ "      ON CONFLICT DO NOTHING\n" //
				+ "    RETURNING id\n" //
				+ "    INTO _channel_id;\n" //
				+ "\n" //
				+ "    EXIT WHEN FOUND;\n" //
				+ "  END LOOP;\n" //
				+ "END;\n" //
				+ "$$;\n\n";
	}

	private String createFunctionGetOrCreateEdgeChannelId() {
		return "CREATE OR REPLACE FUNCTION openems_get_or_create_edge_channel_id(\n" //
				+ "  _edge text,\n" //
				+ "  _channelAddress text,\n" //
				+ "  _type int,\n" //
				+ "  OUT _channel_id int,\n" //
				+ "  OUT _channel_type int,\n" //
				+ "  OUT _available_since TIMESTAMPTZ\n" //
				+ ") LANGUAGE plpgsql AS \n" //
				+ "$$ \n" //
				+ "BEGIN\n" //
				+ "  LOOP\n" //
				+ "    SELECT edge_channel.id, edge_channel.type, edge_channel.available_since\n" //
				+ "    FROM edge_channel\n" //
				+ "    LEFT JOIN edge\n" //
				+ "    ON edge_channel.edge_id = edge.id \n" //
				+ "    LEFT JOIN channel\n" //
				+ "    ON edge_channel.channel_id = channel.id\n" //
				+ "    WHERE edge.name = _edge AND channel.address = _channelAddress\n" //
				+ "    INTO _channel_id, _channel_type, _available_since;\n" //
				+ "\n" //
				+ "    EXIT WHEN FOUND;\n" //
				+ "\n" //
				+ "    SELECT c._channel_id\n" //
				+ "    FROM openems_get_or_create_channel_id(_channelAddress) c\n" //
				+ "    INTO _channel_id;\n" //
				+ "\n" //
				+ "    INSERT INTO edge_channel (edge_id, channel_id, type, available_since)\n" //
				+ "    VALUES ((SELECT _edge_id FROM openems_get_or_create_edge_id(_edge)),\n" //
				+ "      _channel_id,\n" //
				+ "      _type,\n" //
				+ "      now())\n" //
				+ "    ON CONFLICT DO NOTHING\n" //
				+ "    RETURNING id, type\n" //
				+ "    INTO _channel_id, _channel_type;\n" //
				+ "\n" //
				+ "    EXIT WHEN FOUND;\n" //
				+ "  END LOOP;\n" //
				+ "END;\n" //
				+ "$$;\n\n";
	}
}
