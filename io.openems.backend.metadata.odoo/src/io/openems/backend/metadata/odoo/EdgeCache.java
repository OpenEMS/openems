package io.openems.backend.metadata.odoo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.openems.backend.common.metadata.Edge;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.postgres.PgUtils;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.SemanticVersion;

public class EdgeCache {

	public static final int EXPECTED_CACHE_SIZE = 1_000;

	private final MetadataOdoo parent;

	/**
	 * Map Edge-ID (String) to Edge. Initialized with expected cache size.
	 */
	private final Map<String, MyEdge> edgeIdToEdge = new HashMap<>(EXPECTED_CACHE_SIZE);

	/**
	 * Map Odoo-ID (Integer) to Edge-ID (String). Initialized with expected cache
	 * size.
	 */
	private final Map<Integer, String> odooIdToEdgeId = new HashMap<>(EXPECTED_CACHE_SIZE);

	/**
	 * Map Apikey (String) to Edge-ID (String). Initialized with expected cache
	 * size.
	 */
	private final Map<String, String> apikeyToEdgeId = new HashMap<>(EXPECTED_CACHE_SIZE);

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
	public synchronized MyEdge addOrUpdate(ResultSet rs) throws SQLException, OpenemsException {
		// simple fields
		var edgeId = PgUtils.getAsString(rs, EdgeDevice.NAME);
		var odooId = PgUtils.getAsInt(rs, EdgeDevice.ID);
		var apikey = PgUtils.getAsString(rs, EdgeDevice.APIKEY);

		// more simple fields
		var comment = PgUtils.getAsStringOrElse(rs, EdgeDevice.COMMENT, "");
		var version = PgUtils.getAsStringOrElse(rs, EdgeDevice.OPENEMS_VERSION, "");
		var producttype = PgUtils.getAsStringOrElse(rs, EdgeDevice.PRODUCTTYPE, "");
		var lastmessage = PgUtils.getAsDateOrElse(rs, EdgeDevice.LASTMESSAGE, null);

		var edge = this.edgeIdToEdge.get(edgeId);
		if (edge == null) {
			// This is new -> create instance of Edge
			edge = new MyEdge(this.parent, odooId, edgeId, apikey, comment, version, producttype, lastmessage);
			this.edgeIdToEdge.put(edgeId, edge);
			this.odooIdToEdgeId.put(odooId, edgeId);
			this.apikeyToEdgeId.put(apikey, edgeId);
		} else {
			// Edge exists -> update information
			edge.setComment(comment);
			edge.setVersion(SemanticVersion.fromStringOrZero(version));
			edge.setProducttype(producttype);
			edge.setLastmessage(lastmessage);
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

}
