package io.openems.edge.common.user;


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
}
