package io.openems.backend.metadata.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class User {
	private final int id;
	private String name;
	private final Map<Integer, Role> deviceRoles = new HashMap<>();	
	
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
	
	public void addDeviceRole(int deviceId, Role role) {
		this.deviceRoles.put(deviceId, role);
	}
	
	public Map<Integer, Role> getDeviceRoles() {
		return Collections.unmodifiableMap(this.deviceRoles);
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", name=" + name + ", deviceRole=" + deviceRoles + "]";
	}
}
