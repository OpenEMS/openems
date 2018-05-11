package io.openems.common.session;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public enum Role {
	ADMIN, INSTALLER, OWNER, GUEST;

	/**
	 * Returns the Role ENUM for this name or "GUEST" if it was not found.
	 * 
	 * @param name
	 * @return
	 */
	public static Role getRole(String name) {
		switch (name.toLowerCase()) {
		case "admin":
			return ADMIN;
		case "installer":
			return INSTALLER;
		case "owner":
			return OWNER;
		case "guest":
		default:
			return GUEST;
		}
	}

	public static Role getDefaultRole() {
		return GUEST;
	}

	public JsonElement asJson() {
		return new JsonPrimitive(this.name().toLowerCase());
	}
}
