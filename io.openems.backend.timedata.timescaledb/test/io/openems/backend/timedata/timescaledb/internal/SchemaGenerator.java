package io.openems.backend.timedata.timescaledb.internal;

import java.util.stream.Collectors;
import java.util.stream.Stream;

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
				.append(this.createComponentTable()) //
				.append(this.createChannelTable()) //
				.append(this.createEdgeChannelTable()) //
				.append(this.createEdgeChannelIndex()) //
				.append("\n/* Create data tables */\n") //
				.append(this.createDataTables()) //
				.append("\n/* Create PL/pgSQL functions */\n\n") //
				.append(this.createFunctionGetOrCreateEdgeId()) //
				.append(this.createFunctionGetOrCreateComponentId()) //
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

	private String createComponentTable() {
		return """
			CREATE TABLE IF NOT EXISTS component (
			  id SERIAL primary key,
			  name text NOT NULL,
			  UNIQUE(name)
			);
			""";
	}

	private String createChannelTable() {
		return """
			CREATE TABLE IF NOT EXISTS channel (
			  id SERIAL primary key,
			  component_id INTEGER NOT NULL REFERENCES component,
			  name text NOT NULL,
			  priority INTEGER NOT NULL,
			  UNIQUE(component_id, name)
			);
			""";
	}

	private String createEdgeChannelTable() {
		return """
			CREATE TABLE IF NOT EXISTS edge_channel (
			  id SERIAL primary key,
			  edge_id INTEGER NOT NULL REFERENCES edge,
			  channel_id INTEGER NOT NULL REFERENCES channel,
			  type INTEGER NOT NULL,
			  available_since TIMESTAMPTZ
			);
			""";
	}

	private String createEdgeChannelIndex() {
		return "CREATE INDEX ix_edge_channel ON edge_channel (edge_id, channel_id);\n\n";
	}

	private String createDataTables() {
		var sb = new StringBuilder();
		for (var type : Type.values()) {
			sb.append("\n/*   Type: " + type + " */\n");
			for (var priority : Priority.values()) {
				sb //
						.append("\n/*     Priority: " + priority + " */\n\n") //
						.append(this.createRawTable(type, priority)) //
						.append(this.createHyperTable(type, priority)) //
						.append(this.createRawIndex(type, priority)) //
						// .append(this.alterCompression(type, priority)) //
						// .append(this.addCompression(type, priority)) //
						.append(this.createAggregateTable(type, priority)) //
						.append(this.addContinuousAggregate(type, priority)) //
				;
			}
		}
		return sb.toString();
	}

	private String createRawTable(Type type, Priority priority) {
		return "CREATE TABLE IF NOT EXISTS " + type.getRawTableName(priority) + " (\n" //
				+ "  time TIMESTAMPTZ (3) NOT NULL,\n" //
				+ "  edge_channel_id INTEGER NOT NULL,\n" //
				+ "  value " + type.sqlDataType + " NOT NULL\n" //
				+ ");\n\n";
	}

	private String createHyperTable(Type type, Priority priority) {
		return "SELECT create_hypertable('" + type.getRawTableName(priority) + "',\n" //
				+ "  'time',\n" //
				+ "  chunk_time_interval => interval '1 day',\n" //
				+ "  create_default_indexes => false);\n\n";
	}

	private String createRawIndex(Type type, Priority priority) {
		return "CREATE INDEX ix_" + type.getRawTableName(priority) + "_time_channel ON " //
				+ type.getRawTableName(priority) + "(time desc, edge_channel_id ASC);\n\n";
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

	private String createAggregateTable(Type type, Priority priority) {
		return "CREATE MATERIALIZED VIEW " + type.getAggregate5mTableName(priority) //
				+ "(time, edge_channel_id, " //
				+ Stream.of(type.aggregateFunctions) //
						.map(s -> "\"" + s + "\"") //
						.collect(Collectors.joining(", "))
				+ ")\n" //
				+ "  WITH (timescaledb.continuous) AS\n" //
				+ "  SELECT time_bucket ('5 minutes', time) AS time,\n" //
				+ "    edge_channel_id,\n" //
				+ "    " + Stream.of(type.aggregateFunctions) //
						.map(s -> s + "(\"value\")") //
						.collect(Collectors.joining(", "))
				+ "\n" //
				+ "  FROM " + type.getRawTableName(priority) + "\n" //
				+ "  GROUP BY (1, 2)\n" + "WITH NO DATA;\n\n";
		// TODO set chunk time interval for materialized view to 1 day
	}

	private String addContinuousAggregate(Type type, Priority priority) {
		return "SELECT add_continuous_aggregate_policy('" + type.getAggregate5mTableName(priority) + "',\n" //
				+ "  start_offset => NULL,\n" //
				// + " start_offset => interval '30 days',\n" // TODO switch to 30 days after
				+ "  end_offset => interval '" + (priority == Priority.HIGH ? "10" : "60") + " minutes',\n" //
				+ "  schedule_interval => interval '" + (priority == Priority.HIGH ? "10" : "60") + " minutes'\n" //
				+ ");\n" //

				+ "SELECT set_chunk_time_interval(\n" //
				+ "  (\n" //
				+ "    SELECT format('%I.%I', materialization_hypertable_schema, materialization_hypertable_name) AS materialization_hypertable\n"
				+ "    FROM timescaledb_information.continuous_aggregates\n" //
				+ "    WHERE view_name LIKE '" + type.getAggregate5mTableName(priority) + "'\n" //
				+ "  ), INTERVAL '1 day'\n" //
				+ ");\n" //

				+ "SELECT alter_job(\n" //
				+ "  (\n" //
				+ "    SELECT job_id\n" //
				+ "    FROM timescaledb_information.jobs j\n" //
				+ "    INNER JOIN timescaledb_information.continuous_aggregates ca\n" //
				+ "    ON j.hypertable_schema = ca.materialization_hypertable_schema\n" //
				+ "      AND j.hypertable_name = ca.materialization_hypertable_name\n" //
				+ "    WHERE ca.view_name like '" + type.getAggregate5mTableName(priority) + "'\n" //
				+ "  ), next_start => now()\n" //
				+ ");\n\n";
	}

	private String createFunctionGetOrCreateEdgeId() {
		return """
			CREATE OR REPLACE FUNCTION openems_get_or_create_edge_id(
			  _edge text,
			  OUT _edge_id int
			) LANGUAGE plpgsql AS\s
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

	private String createFunctionGetOrCreateComponentId() {
		return """
			CREATE OR REPLACE FUNCTION openems_get_or_create_component_id(
			  _component text,\s
			  OUT _component_id int
			) LANGUAGE plpgsql AS\s
			$$
			BEGIN\s\s\s\s
			  loop
			    SELECT component.id
			      FROM component
			      WHERE component.name = _component
			      INTO _component_id;\s\s\s
			
			      EXIT WHEN FOUND;
			
			    INSERT INTO component (name)
			    VALUES (_component)\s
			      ON CONFLICT DO nothing
			      RETURNING id INTO _component_id;
			
			    EXIT WHEN FOUND;
			  END LOOP;
			END;
			$$;
			""";
	}

	private String createFunctionGetOrCreateChannelId() {
		return "CREATE OR REPLACE FUNCTION openems_get_or_create_channel_id(\n" //
				+ "  _component text,\n" //
				+ "  _channel text,\n" //
				+ "  OUT _channel_id int,\n" //
				+ "  OUT _priority int\n" //
				+ ") LANGUAGE plpgsql AS \n" //
				+ "$$ \n" //
				+ "BEGIN\n" //
				+ "  LOOP\n" //
				+ "    SELECT channel.id, channel.priority\n" //
				+ "    FROM channel\n" //
				+ "    LEFT JOIN component\n" //
				+ "    ON channel.component_id = component.id \n" //
				+ "    WHERE component.name = _component AND channel.name = _channel\n" //
				+ "    INTO _channel_id, _priority;\n" //
				+ "\n" //
				+ "    EXIT WHEN FOUND;\n" //
				+ "\n" //
				+ "    INSERT INTO channel (component_id, name, priority)\n" //
				+ "    VALUES ((SELECT _component_id FROM openems_get_or_create_component_id(_component)), _channel, "
				+ Priority.LOW.getId() + "" + ")\n" //
				+ "      ON CONFLICT DO NOTHING\n" //
				+ "    RETURNING id, priority\n" //
				+ "    INTO _channel_id, _priority;\n" //
				+ "\n" //
				+ "    EXIT WHEN FOUND;\n" //
				+ "  END LOOP;\n" //
				+ "END;\n" //
				+ "$$;\n\n";
	}

	private String createFunctionGetOrCreateEdgeChannelId() {
		return """
			CREATE OR REPLACE FUNCTION openems_get_or_create_edge_channel_id(
			  _edge text,
			  _component text,
			  _channel text,
			  _type int,
			  OUT _channel_id int,
			  OUT _channel_type int,
			  OUT _priority int,
			  OUT _available_since TIMESTAMPTZ
			) LANGUAGE plpgsql AS\s
			$$\s
			BEGIN
			  LOOP
			    SELECT edge_channel.id, edge_channel.type, channel.priority, edge_channel.available_since
			    FROM edge_channel
			    LEFT JOIN edge
			    ON edge_channel.edge_id = edge.id\s
			    LEFT JOIN channel
			    ON edge_channel.channel_id = channel.id
			    LEFT JOIN component
			    ON channel.component_id = component.id\s
			    WHERE edge.name = _edge AND component.name = _component AND channel.name = _channel
			    INTO _channel_id, _channel_type, _priority, _available_since;
			
			    EXIT WHEN FOUND;
			
			    SELECT c._channel_id, c._priority
			    FROM openems_get_or_create_channel_id(_component, _channel) c
			    INTO _channel_id, _priority;
			
			    INSERT INTO edge_channel (edge_id, channel_id, type, available_since)
			    VALUES ((SELECT _edge_id FROM openems_get_or_create_edge_id(_edge)),
			      _channel_id,
			      _type,
			      now())
			    ON CONFLICT DO NOTHING
			    RETURNING id, type
			    INTO _channel_id, _channel_type;
			
			    EXIT WHEN FOUND;
			  END LOOP;
			END;
			$$;
			""";
	}
}
