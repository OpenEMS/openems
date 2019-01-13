package io.openems.backend.metadata.api;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import io.openems.common.session.Role;

/**
 * Represents a Backend-User within Metadata Service.
 */
public class User {

	private final String id;
	private final String name;
	private final NavigableMap<String, Role> edgeRoles = new TreeMap<>();

	public User(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/**
	 * Sets the Role for a given Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @param role   the Role
	 */
	public void addEdgeRole(String edgeId, Role role) {
		this.edgeRoles.put(edgeId, role);
	}

	/**
	 * Gets all Roles for Edge-IDs.
	 * 
	 * @return the map of Roles
	 */
	public NavigableMap<String, Role> getEdgeRoles() {
		return Collections.unmodifiableNavigableMap(this.edgeRoles);
	}

	/**
	 * Gets the Role for a given Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @return the Role
	 */
	public Optional<Role> getEdgeRole(String edgeId) {
		return Optional.ofNullable(this.edgeRoles.get(edgeId));
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", edgeRole=" + edgeRoles + "]";
	}
}
