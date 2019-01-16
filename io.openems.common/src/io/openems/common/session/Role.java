package io.openems.common.session;

import com.google.gson.JsonPrimitive;

public enum Role {
	/*
	 * The System Administrator
	 */
	ADMIN(0), //
	/*
	 * A qualified person with increased permissions
	 */
	INSTALLER(1), //
	/*
	 * The Owner/End-customer of the system
	 */
	OWNER(2), //
	/*
	 * A Guest with Read-Only permission
	 */
	GUEST(3);

	private final int level;

	private Role(int level) {
		this.level = level;
	}

	/**
	 * The Level of this Role. The lower the better.
	 * 
	 * @return the level
	 */
	public int getLevel() {
		return level;
	}

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
	 * Gets the information whether the current Role is equal or more privileged
	 * than the given Role.
	 * 
	 * @param role the compared Role
	 * @return true if the current Role privileges are  equal or higher
	 */
	public boolean isAtLeast(Role role) {
		return this.level <= role.level;
	}

	/**
	 * Gets the information whether the current Role is less privileged than the
	 * given Role.
	 * 
	 * @param role the compared Role
	 * @return true if the current Role is less privileged
	 */
	public boolean isLessThan(Role role) {
		return this.level > role.level;
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
