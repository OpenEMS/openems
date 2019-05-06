package io.openems.edge.common.user;


import io.openems.edge.common.access_control.Role;

import java.util.Optional;

// TODO evaluate org.osgi.service.useradmin.User;

public interface UserService {
	/**
	 * Authenticates a user with his password
	 *
	 * @param password
	 * @return the authenticated User or Empty if authentication failed
	 */
	Optional<EdgeUser> authenticate(String password);

	/**
	 * Authenticates a user with his username and password
	 *
	 * @param password
	 * @return the authenticated User or Empty if authentication failed
	 */
	Optional<EdgeUser> authenticate(String username, String password);

	/**
	 * Authenticates a user with his password
	 *
	 * @param password
	 * @return the authenticated User or Empty if authentication failed
	 */
	Role authenticate2(String password);

	/**
	 * Authenticates a user with his username and password
	 *
	 * @param password
	 * @return the authenticated User or Empty if authentication failed
	 */
	Role authenticate2(String username, String password);
}
