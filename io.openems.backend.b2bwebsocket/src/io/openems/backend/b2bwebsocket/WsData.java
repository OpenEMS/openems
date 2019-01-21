package io.openems.backend.b2bwebsocket;

import java.util.Optional;

import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class WsData extends io.openems.common.websocket.WsData {

	private User user = null;

	public void setUser(User user) {
		this.user = user;
	}

	public Optional<User> getUser() {
		return Optional.ofNullable(user);
	}

	/**
	 * Gets the authenticated User or throws an Exception if User is not
	 * authenticated.
	 * 
	 * @return the User
	 * @throws OpenemsNamedException if User is not authenticated
	 */
	public User assertUser() throws OpenemsNamedException {
		Optional<User> userOpt = this.getUser();
		if (!userOpt.isPresent()) {
			OpenemsError.COMMON_USER_NOT_AUTHENTICATED.exception("");
		}
		return userOpt.get();
	}

}
