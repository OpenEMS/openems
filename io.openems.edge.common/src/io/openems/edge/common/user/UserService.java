package io.openems.edge.common.user;

import java.util.Optional;

public interface UserService {

	/**
	 * Authenticates a user with his password.
	 *
	 * @param password the password
	 * @return the authenticated User or Empty if authentication failed
	 */
	Optional<User> authenticate(String password);

	/**
	 * Authenticates a user with his username and password.
	 *
	 * @param username the username
	 * @param password the password
	 * @return the authenticated User or Empty if authentication failed
	 */
	Optional<User> authenticate(String username, String password);

}
