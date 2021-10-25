package io.openems.edge.common.test;

import java.util.Optional;

import io.openems.edge.common.user.User;
import io.openems.edge.common.user.UserService;

/**
 * Simulates a {@link UserService} for the OpenEMS Component test framework.
 */
public class DummyUserService implements UserService {

	private final DummyUser[] users;

	public DummyUserService(DummyUser... users) {
		this.users = users;
	}

	@Override
	public Optional<User> authenticate(String password) {
		for (DummyUser user : this.users) {
			if (user.password.equals(password)) {
				return Optional.of(user);
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<User> authenticate(String username, String password) {
		for (DummyUser user : this.users) {
			if (user.getId().equals(username) && user.password.equals(password)) {
				return Optional.of(user);
			}
		}
		return Optional.empty();
	}

}