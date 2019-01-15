package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;

import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.metadata.api.User;

public class WsData extends io.openems.common.websocket.WsData {

	private final SubscribedChannelsWorker subscribedChannelsWorker;
	private Optional<String> userId = Optional.empty();
	private Optional<UUID> token = Optional.empty();

	public WsData(UiWebsocketImpl parent) {
		this.subscribedChannelsWorker = new SubscribedChannelsWorker(parent, this);
	}

	@Override
	public void dispose() {
		this.subscribedChannelsWorker.dispose();
	}

	public synchronized void setUserId(String userId) {
		this.userId = Optional.ofNullable(userId);
	}

	/**
	 * Gets the authenticated User-ID.
	 * 
	 * @return the User-ID or Optional.Empty if the User was not authenticated.
	 */
	public synchronized Optional<String> getUserId() {
		return userId;
	}

	/**
	 * Gets the authenticated User.
	 * 
	 * @param metadata the Metadata service
	 * @return the User or Optional.Empty if the User was not authenticated or it is
	 *         not available from Metadata service
	 */
	public synchronized Optional<User> getUser(Metadata metadata) {
		Optional<String> userId = this.getUserId();
		if (userId.isPresent()) {
			Optional<User> user = metadata.getUser(userId.get());
			return user;
		}
		return Optional.empty();
	}

	public void setToken(UUID token) {
		this.token = Optional.ofNullable(token);
	}

	public Optional<UUID> getToken() {
		return token;
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
