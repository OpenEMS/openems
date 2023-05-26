package io.openems.common.session;

import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

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
		return this.level;
	}

	/**
	 * Returns the Role ENUM for this name or "GUEST" if it was not found.
	 *
	 * @param name the name of the Role
	 * @return the Role
	 */
	public static Role getRole(String name) {
		return switch (name.toLowerCase()) {
		 		case "admin" -> ADMIN;
		 		case "installer" ->INSTALLER;
		 		case "owner"-> OWNER;
		 		case "guest" -> GUEST;
		 		default -> GUEST;
			 
		};
	}

	/**
	 * Gets the information whether the current Role is equal or more privileged
	 * than the given Role.
	 *
	 * @param role the compared Role
	 * @return true if the current Role privileges are equal or higher
	 */
	public boolean isAtLeast(Role role) {
		return this.level <= role.level;
	}

	/**
	 * Throws an exception if the current Role is equal or more privileged than the
	 * given Role.
	 *
	 * @param resource a resource identifier; used for the exception
	 * @param role     the compared Role
	 * @return the current Role
	 * @throws OpenemsNamedException if the current Role privileges are less
	 */
	public Role assertIsAtLeast(String resource, Role role) throws OpenemsNamedException {
		if (this.isAtLeast(role)) {
			// Ok
			return this;
		}
		throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception(resource, this.toString());
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
