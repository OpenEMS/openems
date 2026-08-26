package io.openems.edge.common.test;

import java.util.Arrays;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.session.Language;
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

	@Override
	public void registerAdminUser(String setupKey, String username, String password, Language language)
			throws OpenemsError.OpenemsNamedException {
		throw new OpenemsError.OpenemsNamedException(OpenemsError.COMMON_AUTHENTICATION_FAILED);
	}

	@Override
	public void updateLanguage(Language language) {
		for (DummyUser user : this.users) {
			user.setLanguage(language);
		}
	}

	@Override
	public Optional<User> getUserById(String userId) {
		return Arrays.stream(this.users) //
				.filter(user -> user.getId().equals(userId)) //
				.<User>map(t -> t) //
				.findFirst();
	}

}