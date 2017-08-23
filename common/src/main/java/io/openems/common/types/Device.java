package io.openems.common.types;

import com.google.gson.JsonObject;

import io.openems.common.session.Role;

/**
 * Helper class to store tuple of device name and role
 *
 * @author stefan.feilmeier
 *
 */
public class Device {
	private final String name;
	private final Role role;
	private boolean online = false;
	
	public Device(String name, String role) {
		this.name = name;
		this.role = Role.getRole(role);
	}

	public String getName() {
		return name;
	}

	public Role getRole() {
		return role;
	}
	
	public void setOnline(boolean online) {
		this.online = online;
	}
	
	public boolean isOnline() {
		return online;
	}
}
