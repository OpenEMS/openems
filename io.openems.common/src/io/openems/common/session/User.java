package io.openems.common.session;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a User.
 */
public class User {

	public static User from(JsonObject j) throws OpenemsNamedException {
		String id = JsonUtils.getAsString(j, "id");
		String name = JsonUtils.getAsString(j, "name");
		String role = JsonUtils.getAsString(j, "role");
		return new User(id, name, Role.getRole(role));
	}

	private final String id;
	private final String name;
	private final Role role;

	public User(String id, String name, Role role) {
		this.id = id;
		this.name = name;
		this.role = role;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Role getRole() {
		return role;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", name=" + name + ", role=" + role + "]";
	}

	public JsonObject toJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("id", this.id) //
				.addProperty("name", this.name) //
				.addProperty("role", this.role.toString()) //
				.build();
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
	public Role assertRoleIsAtLeast(String resource, Role role) throws OpenemsNamedException {
		if (this.role == null) {
			throw OpenemsError.COMMON_ROLE_UNDEFINED.exception(this.getId());
		}
		if (!this.role.isAtLeast(role)) {
			throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception(resource, this.role.toString());
		}
		return this.role;
	}

}
