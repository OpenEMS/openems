package io.openems.edge.api.user;

import java.util.Optional;

// TODO evaluate org.osgi.service.useradmin.User;

public interface UserService {
	/**
	 * Authenticates a user with his password
	 *
	 * @param password
	 * @return the authenticated User or Empty if authentication failed
	 */
	Optional<User> authenticate(String password);

	/**
	 * Authenticates a user with his username and password
	 *
	 * @param password
	 * @return the authenticated User or Empty if authentication failed
	 */
	Optional<User> authenticate(String username, String password);
}
