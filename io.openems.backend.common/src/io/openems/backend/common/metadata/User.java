package io.openems.backend.common.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.response.AuthenticateWithPasswordResponse.EdgeMetadata;
import io.openems.common.session.AbstractUser;
import io.openems.common.session.Role;

/**
 * A {@link User} used by OpenEMS Backend.
 */
public class User extends AbstractUser {

	public User(String id, String name, Role globalRole, NavigableMap<String, Role> roles) {
		super(id, name, globalRole, roles);
	}

	/**
	 * Gets the information whether the Users Role for the given Edge is equal or
	 * more privileged than the given Role.
	 * 
	 * @param edgeId the Edge-Id
	 * @param role   the compared Role
	 * @return true if the Users Role privileges are equal or higher
	 */
	public boolean roleIsAtLeast(String edgeId, Role role) {
		Optional<Role> thisRoleOpt = this.getRole(edgeId);
		if (!thisRoleOpt.isPresent()) {
			return false;
		}
		return true;
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

	/**
	 * Gets the Metadata information of the accessible Edges.
	 * 
	 * @param metadataService a {@link Metadata} provider
	 * @return a list of {@link EdgeMetadata}
	 */
	public static List<EdgeMetadata> generateEdgeMetadatas(User user, Metadata metadataService) {
		List<EdgeMetadata> metadatas = new ArrayList<>();
		for (Entry<String, Role> edgeRole : user.getEdgeRoles().entrySet()) {
			String edgeId = edgeRole.getKey();
			Role role = edgeRole.getValue();
			Optional<Edge> edgeOpt = metadataService.getEdge(edgeId);
			if (edgeOpt.isPresent()) {
				Edge e = edgeOpt.get();
				metadatas.add(new EdgeMetadata(//
						e.getId(), // Edge-ID
						e.getComment(), // Comment
						e.getProducttype(), // Product-Type
						e.getVersion(), // Version
						role, // Role
						e.isOnline() // Online-State
				));
			}
		}
		return metadatas;
	}
}
