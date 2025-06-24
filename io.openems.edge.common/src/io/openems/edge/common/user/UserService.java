package io.openems.edge.common.user;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

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

	/**
	 * Registers a new user with the provided username and password if the setupKey
	 * is from this edge. The setupKey gets validated against the backend.
	 * 
	 * @param setupKey the setupKey if this edge
	 * @param username the username of the new user
	 * @param password the password of the new user
	 * @throws OpenemsNamedException on authentication error
	 */
	void registerAdminUser(String setupKey, String username, String password) throws OpenemsNamedException;

}
