package io.openems.backend.metadata.api;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

public class User {
	private final int id;
	private String name;
	private final NavigableMap<Integer, Role> edgeRoles = new TreeMap<>();	
	
	public User(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
	}
	
	public void addEdgeRole(int deviceId, Role role) {
		this.edgeRoles.put(deviceId, role);
	}
	
	public NavigableMap<Integer, Role> getEdgeRoles() {
		return Collections.unmodifiableNavigableMap(this.edgeRoles);
	}
	
	public Optional<Role> getEdgeRole(int edgeId) {
		return Optional.ofNullable(this.edgeRoles.get(edgeId));
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", name=" + name + ", edgeRole=" + edgeRoles + "]";
	}
}
