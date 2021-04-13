package io.openems.edge.common.user;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.session.User;

/**
 * A {@link User} used by OpenEMS Edge.
 */
public abstract class EdgeUser extends User {

	/**
	 * Constructs an {@link EdgeUser}.
	 * 
	 * @param id   the User-ID
	 * @param name the name
	 * @param role the {@link Role}; used as global Role and assigned to
	 *             {@link User#DEFAULT_EDGE_ID}.
	 */
	protected EdgeUser(String id, String name, Role role) {
		super(id, name, role, ImmutableSortedMap.<String, Role>naturalOrder().put(User.DEFAULT_EDGE_ID, role).build());
	}

	/**
	 * Throws an exception if the Role (Global and Per-Edge-Role are the same for
	 * {@link EdgeUser}) is equal or more privileged than the given Role.
	 * 
	 * @param resource a resource identifier; used for the exception
	 * @param role     the compared {@link Role}
	 * @throws OpenemsNamedException if the global Role privileges are less
	 */
	public void assertRoleIsAtLeast(String resource, Role role) throws OpenemsNamedException {
		this.getGlobalRole().assertIsAtLeast(resource, role);
	}

}
