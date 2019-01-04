package io.openems.common.session;

import com.google.gson.JsonPrimitive;

public enum Role {
	ADMIN, INSTALLER, OWNER, GUEST;

	/**
	 * Returns the Role ENUM for this name or "GUEST" if it was not found.
	 * 
	 * @param name the name of the Role
	 * @return the Role
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

	/**
	 * Gets the default Role.
	 * 
	 * @return the Role
	 */
	public static Role getDefaultRole() {
		return GUEST;
	}

	/**
	 * Gets the Role as a JsonPrimitive.
	 * 
	 * @return the JsonPrimitive
	 */
	public JsonPrimitive asJson() {
		return new JsonPrimitive(this.name().toLowerCase());
	}
}
