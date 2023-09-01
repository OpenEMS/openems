package io.openems.common.session;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

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
	 * The {@link Language}.
	 */
	private Language language = Language.DEFAULT;

	/**
	 * Roles per Edge-ID.
	 */
	private final NavigableMap<String, Role> roles = new TreeMap<>();

	protected AbstractUser(String id, String name, Language language, Role globalRole,
			NavigableMap<String, Role> roles) {
		this.id = id;
		this.name = name;
		this.language = language;
		this.globalRole = globalRole;
	}

	public String getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	/**
	 * Gets the user language.
	 *
	 * @return the language
	 */
	public Language getLanguage() {
		return this.language;
	}

	/**
	 * Sets the user language.
	 * 
	 * @param language the {@link Language}
	 */
	public void setLanguage(Language language) {
		this.language = language;
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
	 * Gets the Number of Devices, that the user is allowed to see.
	 * 
	 * @return the numberOfDevices
	 */
	public abstract boolean hasMultipleEdges();

}
