package io.openems.backend.metadata.odoo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.Edge;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.Field.EdgeDeviceUserRole;
import io.openems.backend.metadata.odoo.postgres.PgUtils;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.EdgeConfig;
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

	/**
	 * Map Odoo-ID (Integer) to EdgeUser.
	 */
	private final Map<Integer, MyEdgeUser> odooIdToEdgeUser = new HashMap<>();

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

		// config
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

		// more simple fields
		var comment = PgUtils.getAsStringOrElse(rs, EdgeDevice.COMMENT, "");
		var version = PgUtils.getAsStringOrElse(rs, EdgeDevice.OPENEMS_VERSION, "");
		var productType = PgUtils.getAsStringOrElse(rs, EdgeDevice.PRODUCT_TYPE, "");
		int sumStateInt = PgUtils.getAsIntegerOrElse(rs, EdgeDevice.OPENEMS_SUM_STATE, -1);
		var sumState = Level.fromValue(sumStateInt).orElse(null);
		ZonedDateTime lastMessage = PgUtils.getAsDateOrElse(rs, EdgeDevice.LAST_MESSAGE, null);
		ZonedDateTime lastUpdate = PgUtils.getAsDateOrElse(rs, EdgeDevice.LAST_UPDATE, null);

		var edge = this.edgeIdToEdge.get(edgeId);
		if (edge == null) {
			// This is new -> create instance of Edge
			edge = new MyEdge(this.parent, odooId, edgeId, apikey, comment, version, productType, sumState, config,
					lastMessage, lastUpdate);
			this.edgeIdToEdge.put(edgeId, edge);
			this.odooIdToEdgeId.put(odooId, edgeId);
			this.apikeyToEdgeId.put(apikey, edgeId);
		} else {
			// Edge exists -> update information
			edge.setComment(comment);
			edge.setVersion(SemanticVersion.fromStringOrZero(version), false);
			edge.setProducttype(productType);
			edge.setSumState(sumState, false);
			edge.setConfig(config, false);
		}

		return edge;
	}

	/**
	 * Adds a EdgeUser to an existing Edge from a SQL ResultSet.
	 *
	 * @param rs     the ResultSet record
	 * @param edgeId of the Edge to add the User to
	 * @return the new or updated EdgeUser instance
	 * @throws SQLException on error
	 */
	public synchronized MyEdgeUser addOrUpdateUser(ResultSet rs, String edgeId) throws SQLException {
		// simple fields
		int id = PgUtils.getAsInt(rs, EdgeDeviceUserRole.ID);
		String userId = PgUtils.getAsStringOrElse(rs, EdgeDeviceUserRole.USER_ID, null);
		int timeToWait = PgUtils.getAsIntegerOrElse(rs, EdgeDeviceUserRole.TIME_TO_WAIT, 0);
		ZonedDateTime lastNotification = PgUtils.getAsDateOrElse(rs, EdgeDeviceUserRole.LAST_NOTIFICATION, null);

		MyEdgeUser edgeUser = this.odooIdToEdgeUser.get(id);
		if (edgeUser == null) {
			// This is new -> create instance of EdgeUser
			edgeUser = new MyEdgeUser(this.parent, id, edgeId, userId, timeToWait, lastNotification);
			this.getEdgeFromEdgeId(edgeId).addUser(edgeUser);
			this.odooIdToEdgeUser.put(id, edgeUser);
		} else {
			// EdgeUser exists -> update information
			edgeUser.setTimeToWait(timeToWait);
			edgeUser.setLastNotification(lastNotification);
		}

		return edgeUser;
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
	 * Gets an EdgeUser from its Odoo-ID.
	 *
	 * @param odooId the Odoo-ID
	 * @return the EdgeUser, or null
	 */
	public synchronized MyEdgeUser getEdgeUserFromOdooId(int odooId) {
		return this.odooIdToEdgeUser.get(odooId);
	}

}
