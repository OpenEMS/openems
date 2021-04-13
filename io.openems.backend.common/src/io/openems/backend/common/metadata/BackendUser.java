package io.openems.backend.common.metadata;

import java.util.NavigableMap;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.session.User;

/**
 * Represents a Backend-User within Metadata Service.
 */
public class BackendUser extends User {

	public BackendUser(String id, String name, Role role, NavigableMap<String, Role> roles) {
		super(id, name, role, roles);
	}

	/**
	 * Throws an exception if the current Role is equal or more privileged than the
	 * given Role.
	 * 
	 * @param resource a resource identifier; used for the exception
	 * @param edgeId   the Edge-ID
	 * @param role     the compared Role
	 * @return the current Role
	 * @throws OpenemsNamedException if the current Role privileges are less
	 */
	public Role assertEdgeRoleIsAtLeast(String resource, String edgeId, Role role) throws OpenemsNamedException {
		Optional<Role> thisRoleOpt = this.getRole(edgeId);
		if (!thisRoleOpt.isPresent()) {
			throw OpenemsError.COMMON_ROLE_UNDEFINED.exception(this.getId());
		}
		Role thisRole = thisRoleOpt.get();
		if (!thisRole.isAtLeast(role)) {
			throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception(resource, role.toString());
		}
		return thisRole;
	}
}
