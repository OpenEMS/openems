package io.openems.common.session;

import com.google.gson.JsonObject;

/**
 * Represents a User; shared by OpenEMS Backend
 * ('io.openems.backend.common.metadata.User') and Edge
 * ('io.openems.edge.common.user.User').
 */
public abstract class AbstractUser {

	private final String userId;
	/**
	 * The unique User-ID.
	 */
	private final String id;

	private final String email;

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
	 * The user specific settings.
	 */
	private final JsonObject settings;

	protected AbstractUser(String id, String name, Language language, Role globalRole, JsonObject settings) {
		this.id = id;
		this.userId = id;
		this.email = id;
		this.name = name;
		this.language = language;
		this.globalRole = globalRole;
		this.settings = settings == null ? new JsonObject() : settings;
	}

	protected AbstractUser(String userId, String email, String name, Language language, Role globalRole, JsonObject settings) {
		this.id = email;
		this.userId = userId;
		this.email = email;
		this.name = name;
		this.language = language;
		this.globalRole = globalRole;
		this.settings = settings == null ? new JsonObject() : settings;
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
	 * Gets the global Role.
	 *
	 * @return {@link Role}
	 */
	public Role getGlobalRole() {
		return this.globalRole;
	}

	/**
	 * Gets the settings for this user.
	 *
	 * @return the Role
	 */
	public JsonObject getSettings() {
		return this.settings;
	}

	public String getUserId() {
		return this.userId;
	}

	public String getEmail() {
		return this.email;
	}

	/**
	 * Gets the Number of Devices, that the user is allowed to see.
	 * 
	 * @return the numberOfDevices
	 */
	public abstract boolean hasMultipleEdges();

}
