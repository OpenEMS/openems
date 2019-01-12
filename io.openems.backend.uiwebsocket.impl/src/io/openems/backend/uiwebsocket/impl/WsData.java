package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;

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

	public synchronized Optional<String> getUserId() {
		return userId;
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
