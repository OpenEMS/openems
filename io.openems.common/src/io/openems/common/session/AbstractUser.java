package io.openems.common.session;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

/**
 * Represents a User; shared by OpenEMS Backend
 * ('io.openems.backend.common.metadata.User') and Edge
 * ('io.openems.edge.common.user.User').
 */
public abstract class AbstractUser {

	/**
	 * The unique User-ID.
	 */
	private final String id;

	/**
	 * A human readable name.
	 */
	private final String name;

	/**
	 * The Global {@link Role}.
	 */
	private final Role globalRole;

	/**
	 * Roles per Edge-ID.
	 */
	private final NavigableMap<String, Role> roles;

	protected AbstractUser(String id, String name, Role globalRole, NavigableMap<String, Role> roles) {
		this.id = id;
		this.name = name;
		this.globalRole = globalRole;
		this.roles = roles;
	}

	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Gets all Roles for Edge-IDs.
	 * 
	 * @return the map of Roles
	 */
	public NavigableMap<String, Role> getEdgeRoles() {
		return Collections.unmodifiableNavigableMap(this.roles);
	}

	/**
	 * Gets the global Role.
	 * 
	 * @return {@link Role}
	 */
	public Role getGlobalRole() {
		return this.globalRole;
	}

	/**
	 * Gets the Role for a given Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @return the Role
	 */
	public Optional<Role> getRole(String edgeId) {
		return Optional.ofNullable(this.roles.get(edgeId));
	}

	/**
	 * Sets the Role for a given Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @param role   the Role
	 */
	public void setRole(String edgeId, Role role) {
		this.roles.put(edgeId, role);
	}

	/**
	 * Returns the User as a {@link JsonObject}.
	 * 
	 * <p>
	 * 
	 * <pre>
	 * {
	 *  "user": {
	 *    "id": string,
	 *    "name": string,
	 *    "globalRole": {@link Role}
	 *   }
	 * }
	 * </pre>
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		JsonObject roles = new JsonObject();
		for (Entry<String, Role> entry : this.roles.entrySet()) {
			roles.add(entry.getKey(), entry.getValue().asJson());
		}
		return JsonUtils.buildJsonObject() //
				.addProperty("id", this.id) //
				.addProperty("name", this.name) //
				.add("globalRole", this.globalRole.asJson()) //
				.add("roles", roles) //
				.build();
	}

}
