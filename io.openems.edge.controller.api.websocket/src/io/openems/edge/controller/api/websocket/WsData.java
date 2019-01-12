package io.openems.edge.controller.api.websocket;

import java.util.Optional;
import java.util.UUID;

import io.openems.edge.common.user.User;

public class WsData extends io.openems.common.websocket.WsData {

	private final SubscribedChannelsWorker subscribedChannelsWorker;

	/**
	 * The token that is stored in the Browser Cookie. Be aware that this can be
	 * 'null' for a short period of time on open of the websocket.
	 */
	private UUID sessionToken = null;

	private Optional<User> user = Optional.empty();

	public WsData(WebsocketApi parent) {
		this.subscribedChannelsWorker = new SubscribedChannelsWorker(parent, this);
	}

	public void setSessionToken(UUID sessionToken) {
		this.sessionToken = sessionToken;
	}

	public UUID getSessionToken() {
		return sessionToken;
	}

	public void setUser(User user) {
		this.user = Optional.ofNullable(user);
	}

	public void unsetUser() {
		this.user = Optional.empty();
	}

	public Optional<User> getUser() {
		return user;
	}

	/**
	 * Validates if the user is authenticated.
	 * 
	 * @return true if the user is authenticated, false otherwise
	 */
	public boolean isUserAuthenticated() {
		return this.getUser().isPresent();
	}

	/**
	 * Gets the SubscribedChannelsWorker to take care of subscribe to CurrentData.
	 * 
	 * @return the SubscribedChannelsWorker
	 */
	public SubscribedChannelsWorker getSubscribedChannelsWorker() {
		return subscribedChannelsWorker;
	}

}
