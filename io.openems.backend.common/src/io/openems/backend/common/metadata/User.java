package io.openems.backend.common.metadata;

import java.util.NavigableMap;
import java.util.TreeMap;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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
		this(id, name, token, language, globalRole, new TreeMap<>(), hasMultipleEdges, settings);
	}

	public User(String id, String name, String token, Language language, Role globalRole,
			NavigableMap<String, Role> roles, boolean hasMultipleEdges, JsonObject settings) {
		super(id, name, language, globalRole, roles, settings);
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
	 * Throws an exception if the current Role is equal or more privileged than the
	 * given Role.
	 *
	 * @param resource a resource identifier; used for the exception
	 * @param edgeId   the Edge-ID
	 * @param role     the compared Role
	 * @return the current {@link Role}
	 * @throws OpenemsNamedException if the current Role privileges are less
	 */
	public Role assertEdgeRoleIsAtLeast(String resource, String edgeId, Role role) throws OpenemsNamedException {
		var thisRoleOpt = this.getRole(edgeId);
		if (!thisRoleOpt.isPresent()) {
			throw OpenemsError.COMMON_ROLE_UNDEFINED.exception(resource, this.getId());
		}
		var thisRole = thisRoleOpt.get();
		if (!thisRole.isAtLeast(role)) {
			throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception(resource, role.toString());
		}
		return thisRole;
	}

	@Override
	public boolean hasMultipleEdges() {
		return this.hasMultipleEdges;
	}

}
