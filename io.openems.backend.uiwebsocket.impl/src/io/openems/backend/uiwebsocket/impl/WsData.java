package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;

public class WsData extends io.openems.common.websocket.WsData {

	private Optional<BackendCurrentDataWorker> currentDataWorker = Optional.empty();
	private Optional<String> userId = Optional.empty();
	private Optional<UUID> token = Optional.empty();

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

	public void setCurrentDataWorker(BackendCurrentDataWorker currentDataWorker) {
		this.currentDataWorker = Optional.ofNullable(currentDataWorker);
	}

	public Optional<BackendCurrentDataWorker> getCurrentDataWorker() {
		return currentDataWorker;
	}

}
