package io.openems.backend.metadata.odoo;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.openems.backend.metadata.api.Edge;

public class EdgeCache {

	/**
	 * Maps Edge-ID to Edge.
	 */
	private ConcurrentHashMap<String, MyEdge> edgeIdToEdge = new ConcurrentHashMap<>();
	/**
	 * Maps Odoo-ID to Edge-ID.
	 */
	private ConcurrentHashMap<Integer, String> odooIdToEdgeId = new ConcurrentHashMap<>();
	/**
	 * Maps API-Key to Edge-ID.
	 */
	private ConcurrentHashMap<String, String> apikeyToEdgeId = new ConcurrentHashMap<>();

	/**
	 * Adds an Edge to the Cache.
	 * 
	 * @param edge the Edge
	 */
	public void add(MyEdge edge) {
		this.edgeIdToEdge.put(edge.getId(), edge);
		this.odooIdToEdgeId.put(edge.getOdooId(), edge.getId());
		this.apikeyToEdgeId.put(edge.getApikey(), edge.getId());
	}

	/**
	 * Gets an Edge from its Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @return the Edge, or null
	 */
	public MyEdge getEdgeFromEdgeId(String edgeId) {
		return this.edgeIdToEdge.get(edgeId);
	}

	/**
	 * Gets an Edge from its Odoo-ID.
	 * 
	 * @param odooId the Odoo-ID
	 * @return the Edge, or null
	 */
	public MyEdge getEdgeFromOdooId(int odooId) {
		String edgeId = this.odooIdToEdgeId.get(odooId);
		if (edgeId == null) {
			return null;
		}
		return this.getEdgeFromEdgeId(edgeId);
	}

	/**
	 * Gets an Edge-ID from an API-Key.
	 * 
	 * @param apikey the API-Key
	 * @return the Edge-ID, or null
	 */
	public Optional<String> getEdgeIdFromApikey(String apikey) {
		return Optional.ofNullable(this.apikeyToEdgeId.get(apikey));
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
