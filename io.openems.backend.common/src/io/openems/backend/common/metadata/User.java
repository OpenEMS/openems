package io.openems.backend.common.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
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

	public User(String id, String name, String token, Language language, Role globalRole, boolean hasMultipleEdges) {
		this(id, name, token, language, globalRole, new TreeMap<>(), hasMultipleEdges);
	}

	public User(String id, String name, String token, Language language, Role globalRole,
			NavigableMap<String, Role> roles, boolean hasMultipleEdges) {
		super(id, name, language, globalRole, roles);
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

	/**
	 * Gets the Metadata information of the accessible Edges.
	 *
	 * @param user            the {@link User}
	 * @param metadataService a {@link Metadata} provider
	 * @return a list of {@link EdgeMetadata}
	 */
	public static List<EdgeMetadata> generateEdgeMetadatas(User user, Metadata metadataService) {
		List<EdgeMetadata> metadatas = new ArrayList<>();
		for (Entry<String, Role> edgeRole : user.getEdgeRoles().entrySet()) {
			var edgeId = edgeRole.getKey();
			var role = edgeRole.getValue();
			var edgeOpt = metadataService.getEdge(edgeId);
			if (edgeOpt.isPresent()) {
				var edge = edgeOpt.get();
				metadatas.add(new EdgeMetadata(//
						edge.getId(), // Edge-ID
						edge.getComment(), // Comment
						edge.getProducttype(), // Product-Type
						edge.getVersion(), // Version
						role, // Role
						edge.isOnline(), // Online-State
						edge.getLastmessage(), // Last-Message Timestamp
						null, //
						Level.OK //
				));
			}
		}
		return metadatas;
	}

	@Override
	public boolean hasMultipleEdges() {
		return this.hasMultipleEdges;
	}

}
