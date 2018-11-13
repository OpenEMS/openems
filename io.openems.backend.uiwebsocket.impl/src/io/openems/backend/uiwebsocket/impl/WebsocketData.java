package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;

public class WebsocketData {
	// @Nullable
	private Integer userId;
	// @Nullable
	private UUID uuid;
	private Optional<BackendCurrentDataWorker> currentDataWorker = Optional.empty();

	public Integer getUserId() {
		return userId;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setCurrentDataWorker(BackendCurrentDataWorker currentDataWorker) {
		this.currentDataWorker = Optional.ofNullable(currentDataWorker);
	}

	public Optional<BackendCurrentDataWorker> getCurrentDataWorker() {
		return currentDataWorker;
	}

	public void initialize(int userId, UUID uuid) {
		this.userId = userId;
		this.uuid = uuid;
	}
}
