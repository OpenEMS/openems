package io.openems.backend.browserwebsocket;

import com.google.gson.JsonObject;

import io.openems.backend.odoo.info.Role;

/**
 * Helper class to store tuple of device name and role
 *
 * @author stefan.feilmeier
 *
 */
public class DeviceInfo {
	private final String name;
	private final Role role;

	public DeviceInfo(String name, String role) {
		this.name = name;
		this.role = Role.getRole(role);
	}

	public String getName() {
		return name;
	}

	public Role getRole() {
		return role;
	}

	public JsonObject toJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("name", this.name);
		j.addProperty("role", this.role.toString());
		return j;
	}
}
