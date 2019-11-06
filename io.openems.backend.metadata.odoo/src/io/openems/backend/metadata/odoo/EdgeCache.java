package io.openems.backend.metadata.odoo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Edge.State;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.odoo.FieldValue;
import io.openems.backend.metadata.odoo.postgres.PgUtils;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component.JsonFormat;
import io.openems.common.types.EdgeConfigDiff;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;

public class EdgeCache {

	private final Logger log = LoggerFactory.getLogger(EdgeCache.class);
	private final MetadataOdoo parent;

	/**
	 * Map Edge-ID (String) to Edge.
	 */
	private Map<String, MyEdge> edgeIdToEdge = new HashMap<>();

	/**
	 * Map Odoo-ID (Integer) to Edge-ID (String).
	 */
	private Map<Integer, String> odooIdToEdgeId = new HashMap<>();

	/**
	 * Map Apikey (String) to Edge-ID (String).
	 */
	private Map<String, String> apikeyToEdgeId = new HashMap<>();

	public EdgeCache(MetadataOdoo parent) {
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
		String edgeId = PgUtils.getAsString(rs, EdgeDevice.NAME);
		int odooId = PgUtils.getAsInt(rs, EdgeDevice.ID);
		String apikey = PgUtils.getAsString(rs, EdgeDevice.APIKEY);

		// Config
		EdgeConfig config;
		String configString = PgUtils.getAsStringOrElse(rs, EdgeDevice.OPENEMS_CONFIG, "");
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
		String stateString = PgUtils.getAsStringOrElse(rs, EdgeDevice.STATE, State.INACTIVE.name());
		State state;
		try {
			state = State.valueOf(stateString.toUpperCase().replaceAll("-", "_"));
		} catch (IllegalArgumentException e) {
			this.parent.logWarn(this.log,
					"Edge [" + edgeId + "]. Unable to get State from [" + stateString + "]: " + e.getMessage());
			state = State.INACTIVE; // Default
		}

		// more simple fields
		String comment = PgUtils.getAsStringOrElse(rs, EdgeDevice.COMMENT, "");
		String version = PgUtils.getAsStringOrElse(rs, EdgeDevice.OPENEMS_VERSION, "");
		String productType = PgUtils.getAsStringOrElse(rs, EdgeDevice.PRODUCT_TYPE, "");
		String ipv4 = PgUtils.getAsStringOrElse(rs, EdgeDevice.IPV4, "");
		Integer soc = PgUtils.getAsIntegerOrElse(rs, EdgeDevice.SOC, null);

		MyEdge edge = this.edgeIdToEdge.get(edgeId);
		if (edge == null) {
			// This is new -> create instance of Edge and register listeners
			edge = new MyEdge(odooId, edgeId, apikey, comment, state, version, productType, config, soc, ipv4);
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
			edge.setConfig(config, false);
			edge.setSoc(soc, false);
			edge.setIpv4(ipv4, false);
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
		String edgeId = this.odooIdToEdgeId.get(odooId);
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
		String edgeId = this.apikeyToEdgeId.get(apikey);
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
				// Edge came Online
				this.parent.odooHandler.writeEdge(edge,
						new FieldValue<Boolean>(Field.EdgeDevice.OPENEMS_IS_CONNECTED, true));

				if (edge.getState().equals(State.INACTIVE)) {
					// Edge was Inactive -> Update state to active
					this.parent.logInfo(this.log,
							"Mark Edge [" + edge.getId() + "] as ACTIVE. It was [" + edge.getState().name() + "]");
					edge.setState(State.ACTIVE);
					this.parent.odooHandler.writeEdge(edge, new FieldValue<String>(Field.EdgeDevice.STATE, "active"));
				}

			} else {
				// Edge disconnected
				this.parent.odooHandler.writeEdge(edge,
						new FieldValue<Boolean>(Field.EdgeDevice.OPENEMS_IS_CONNECTED, false));
			}
		});
		edge.onSetConfig(config -> {
			// Update Edge config in Odoo
			EdgeConfigDiff diff = EdgeConfigDiff.diff(config, edge.getConfig());
			if (!diff.isDifferent()) {
				return;
			}

			this.parent.logInfo(this.log,
					"Edge [" + edge.getId() + "]. Update config: " + StringUtils.toShortString(diff.toString(), 100));
			String conf = new GsonBuilder().setPrettyPrinting().create().toJson(config.toJson());
			String components = new GsonBuilder().setPrettyPrinting().create()
					.toJson(config.componentsToJson(JsonFormat.WITHOUT_CHANNELS));
			this.parent.odooHandler.writeEdge(edge, //
					new FieldValue<String>(Field.EdgeDevice.OPENEMS_CONFIG, conf),
					new FieldValue<String>(Field.EdgeDevice.OPENEMS_CONFIG_COMPONENTS, components));
			this.parent.getOdooHandler().addChatterMessage(edge, "<p>Configuration Update:</p>" + diff.getAsHtml());
		});
		edge.onSetLastMessage(() -> {
			// Set LastMessage timestamp in Odoo/Postgres
			this.parent.getPostgresHandler().getWriteWorker().onLastMessage(edge);
		});
		edge.onSetLastUpdate(() -> {
			// Set LastUpdate timestamp in Odoo/Postgres
			this.parent.getPostgresHandler().getWriteWorker().onLastUpdate(edge);
		});
		edge.onSetVersion(version -> {
			// Set Version in Odoo
			this.parent.logInfo(this.log, "Edge [" + edge.getId() + "]: Update OpenEMS Edge version to [" + version
					+ "]. It was [" + edge.getVersion() + "]");
			this.parent.odooHandler.writeEdge(edge,
					new FieldValue<String>(Field.EdgeDevice.OPENEMS_VERSION, version.toString()));
		});
		edge.onSetSoc(soc -> {
			// Set SoC in Odoo
			this.parent.odooHandler.writeEdge(edge, new FieldValue<String>(Field.EdgeDevice.SOC, String.valueOf(soc)));
		});
		edge.onSetIpv4(ipv4 -> {
			// Set IPv4 in Odoo
			this.parent.odooHandler.writeEdge(edge,
					new FieldValue<String>(Field.EdgeDevice.IPV4, String.valueOf(ipv4)));
		});
		edge.onSetComponentState(activeStateChannels -> {
			this.parent.postgresHandler.updateDeviceStates(edge, activeStateChannels);
		});
	}

}
