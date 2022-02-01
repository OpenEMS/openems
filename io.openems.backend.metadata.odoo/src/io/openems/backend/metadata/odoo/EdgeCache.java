package io.openems.backend.metadata.odoo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Edge.State;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.odoo.FieldValue;
import io.openems.backend.metadata.odoo.postgres.PgUtils;
import io.openems.backend.metadata.odoo.postgres.task.InsertEdgeConfigUpdate;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeConfig;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeProducttype;
import io.openems.backend.metadata.odoo.postgres.task.UpdateEdgeStateActive;
import io.openems.backend.metadata.odoo.postgres.task.UpdateSumState;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfigDiff;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

public class EdgeCache {

	private final Logger log = LoggerFactory.getLogger(EdgeCache.class);
	private final OdooMetadata parent;

	/**
	 * Map Edge-ID (String) to Edge.
	 */
	private final Map<String, MyEdge> edgeIdToEdge = new HashMap<>();

	/**
	 * Map Odoo-ID (Integer) to Edge-ID (String).
	 */
	private final Map<Integer, String> odooIdToEdgeId = new HashMap<>();

	/**
	 * Map Apikey (String) to Edge-ID (String).
	 */
	private final Map<String, String> apikeyToEdgeId = new HashMap<>();

	public EdgeCache(OdooMetadata parent) {
		this.parent = parent;
	}

	/**
	 * Adds a Edge or Updates an existing Edge from a SQL ResultSet.
	 *
	 * @param rs the ResultSet record
	 * @return the new or updated Edge instance
	 * @throws SQLException     on error
	 * @throws OpenemsException on error
	 */
	public synchronized MyEdge addOrUpate(ResultSet rs) throws SQLException, OpenemsException {
		// simple fields
		var edgeId = PgUtils.getAsString(rs, EdgeDevice.NAME);
		var odooId = PgUtils.getAsInt(rs, EdgeDevice.ID);
		var apikey = PgUtils.getAsString(rs, EdgeDevice.APIKEY);

		// Config
		EdgeConfig config;
		var configString = PgUtils.getAsStringOrElse(rs, EdgeDevice.OPENEMS_CONFIG, "");
		if (configString.isEmpty()) {
			config = new EdgeConfig();
		} else {
			try {
				config = EdgeConfig.fromJson(//
						JsonUtils.getAsJsonObject(//
								JsonUtils.parse(configString)));
			} catch (OpenemsNamedException e) {
				this.parent.logWarn(this.log, "Unable to read Edge-Config for Odoo-ID [" + odooId + "] Edge-ID ["
						+ edgeId + "]: " + e.getMessage());
				config = new EdgeConfig();
			}
		}

		// State
		var stateString = PgUtils.getAsStringOrElse(rs, EdgeDevice.STATE, State.INACTIVE.name());
		State state;
		try {
			state = State.valueOf(stateString.toUpperCase().replace('-', '_'));
		} catch (IllegalArgumentException e) {
			this.parent.logWarn(this.log,
					"Edge [" + edgeId + "]. Unable to get State from [" + stateString + "]: " + e.getMessage());
			state = State.INACTIVE; // Default
		}

		// more simple fields
		var comment = PgUtils.getAsStringOrElse(rs, EdgeDevice.COMMENT, "");
		var version = PgUtils.getAsStringOrElse(rs, EdgeDevice.OPENEMS_VERSION, "");
		var productType = PgUtils.getAsStringOrElse(rs, EdgeDevice.PRODUCT_TYPE, "");
		int sumStateInt = PgUtils.getAsIntegerOrElse(rs, EdgeDevice.OPENEMS_SUM_STATE, -1);
		var sumState = Level.fromValue(sumStateInt).orElse(null);

		var edge = this.edgeIdToEdge.get(edgeId);
		if (edge == null) {
			// This is new -> create instance of Edge and register listeners
			edge = new MyEdge(odooId, edgeId, apikey, comment, state, version, productType, sumState, config);
			this.addListeners(edge);
			this.edgeIdToEdge.put(edgeId, edge);
			this.odooIdToEdgeId.put(odooId, edgeId);
			this.apikeyToEdgeId.put(apikey, edgeId);
		} else {
			// Edge exists -> update information
			edge.setComment(comment);
			edge.setState(state);
			edge.setVersion(SemanticVersion.fromStringOrZero(version), false);
			edge.setProducttype(productType);
			edge.setSumState(sumState, false);
			edge.setConfig(config, false);
		}

		return edge;
	}

	/**
	 * Gets an Edge from its Edge-ID.
	 *
	 * @param edgeId the Edge-ID
	 * @return the Edge, or null
	 */
	public synchronized MyEdge getEdgeFromEdgeId(String edgeId) {
		return this.edgeIdToEdge.get(edgeId);
	}

	/**
	 * Gets an Edge from its Odoo-ID.
	 *
	 * @param odooId the Odoo-ID
	 * @return the Edge, or null
	 */
	public synchronized MyEdge getEdgeFromOdooId(int odooId) {
		var edgeId = this.odooIdToEdgeId.get(odooId);
		if (edgeId == null) {
			return null;
		}
		return this.getEdgeFromEdgeId(edgeId);
	}

	/**
	 * Gets an Edge from its Apikey.
	 *
	 * @param apikey the Apikey
	 * @return the Edge, or null
	 */
	public synchronized MyEdge getEdgeForApikey(String apikey) {
		var edgeId = this.apikeyToEdgeId.get(apikey);
		if (edgeId == null) {
			return null;
		}
		return this.getEdgeFromEdgeId(edgeId);
	}

	/**
	 * Gets all Edges as an unmodifiable Collection.
	 *
	 * @return a collection of Edges
	 */
	public Collection<Edge> getAllEdges() {
		return Collections.unmodifiableCollection(this.edgeIdToEdge.values());
	}

	/**
	 * Adds Listeners to act on changes to Edge.
	 *
	 * @param edge the Edge
	 */
	private void addListeners(MyEdge edge) {
		edge.onSetOnline(isOnline -> {
			if (isOnline) {
				// Set OpenEMS Is Connected in Odoo/Postgres
				this.parent.getPostgresHandler().getPeriodicWriteWorker().isOnline(edge);

				if (edge.getState().equals(State.INACTIVE)) {
					// Edge was Inactive -> Update state to active
					this.parent.logInfo(this.log,
							"Mark Edge [" + edge.getId() + "] as ACTIVE. It was [" + edge.getState().name() + "]");
					edge.setState(State.ACTIVE);
					this.parent.getPostgresHandler().getQueueWriteWorker();
					var queueWriteWorker = this.parent.getPostgresHandler().getQueueWriteWorker();
					queueWriteWorker.addTask(new UpdateEdgeStateActive(edge.getOdooId()));
				}
			} else {
				// Set OpenEMS Is Connected in Odoo/Postgres
				this.parent.getPostgresHandler().getPeriodicWriteWorker().isOffline(edge);
			}
		});
		edge.onSetConfig(config -> {
			// Update Edge Config in Odoo/Postgres
			var diff = EdgeConfigDiff.diff(config, edge.getConfig());
			if (!diff.isDifferent()) {
				return;
			}

			this.parent.logInfo(this.log, "Edge [" + edge.getId() + "]. Update config: " + diff.toString());

			var queueWriteWorker = this.parent.getPostgresHandler().getQueueWriteWorker();
			queueWriteWorker.addTask(new UpdateEdgeConfig(edge.getOdooId(), config));
			queueWriteWorker.addTask(new InsertEdgeConfigUpdate(edge.getOdooId(), diff));
		});
		edge.onSetLastMessage(() -> {
			// Set LastMessage timestamp in Odoo/Postgres
			this.parent.getPostgresHandler().getPeriodicWriteWorker().onLastMessage(edge);
		});
		edge.onSetLastUpdate(() -> {
			// Set LastUpdate timestamp in Odoo/Postgres
			this.parent.getPostgresHandler().getPeriodicWriteWorker().onLastUpdate(edge);
		});
		edge.onSetVersion(version -> {
			// Set Version in Odoo
			this.parent.logInfo(this.log, "Edge [" + edge.getId() + "]: Update OpenEMS Edge version to [" + version
					+ "]. It was [" + edge.getVersion() + "]");
			this.parent.odooHandler.writeEdge(edge,
					new FieldValue<>(Field.EdgeDevice.OPENEMS_VERSION, version.toString()));
		});
		edge.onSetSumState(sumState -> {
			// Set Sum-State in Odoo/Postgres
			this.parent.getPostgresHandler().getQueueWriteWorker()
					.addTask(new UpdateSumState(edge.getOdooId(), sumState));
		});
		edge.onSetProducttype(producttype -> {
			// Set Producttype in Odoo/Postgres
			this.parent.getPostgresHandler().getQueueWriteWorker()
					.addTask(new UpdateEdgeProducttype(edge.getOdooId(), producttype));
		});
	}

}
