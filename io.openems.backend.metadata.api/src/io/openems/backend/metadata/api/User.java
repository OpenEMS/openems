package io.openems.backend.metadata.api;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import io.openems.common.session.Role;

public class User {

	private final String id;
	private final NavigableMap<String, Role> edgeRoles = new TreeMap<>();

	public User(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void addEdgeRole(String edgeId, Role role) {
		this.edgeRoles.put(edgeId, role);
	}

	public NavigableMap<String, Role> getEdgeRoles() {
		return Collections.unmodifiableNavigableMap(this.edgeRoles);
	}

	public Optional<Role> getEdgeRole(String edgeId) {
		return Optional.ofNullable(this.edgeRoles.get(edgeId));
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", edgeRole=" + edgeRoles + "]";
	}
}
