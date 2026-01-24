package io.openems.backend.common.metadata;

import com.google.gson.JsonObject;

import io.openems.common.session.AbstractUser;
import io.openems.common.session.Language;
import io.openems.common.session.Role;

/**
 * A {@link User} used by OpenEMS Backend.
 */
public class User extends AbstractUser {

	/**
	 * Keeps the login token.
	 */
	private final String token;

	/**
	 * True, if the current User can see multiple edges.
	 */
	private final boolean hasMultipleEdges;

	public User(String id, String name, String token, Language language, Role globalRole, boolean hasMultipleEdges,
			JsonObject settings) {
		super(id, name, language, globalRole, settings);
		this.hasMultipleEdges = hasMultipleEdges;
		this.token = token;
	}

	public User(String userId, String email, String name, String token, Language language, Role globalRole,
			boolean hasMultipleEdges, JsonObject settings) {
		super(userId, email, name, language, globalRole, settings);
		this.hasMultipleEdges = hasMultipleEdges;
		this.token = token;
	}

	/**
	 * Gets the login token.
	 *
	 * @return the token
	 */
	public String getToken() {
		return this.token;
	}

	/**
	 * Creates a new User with the given token.
	 *
	 * @param token the token
	 * @return a new User
	 */
	public User withToken(String token) {
		return new User(this.getUserId(), this.getEmail(), this.getName(), token, this.getLanguage(),
				this.getGlobalRole(), this.hasMultipleEdges(), this.getSettings());
	}

	@Override
	public boolean hasMultipleEdges() {
		return this.hasMultipleEdges;
	}

}
