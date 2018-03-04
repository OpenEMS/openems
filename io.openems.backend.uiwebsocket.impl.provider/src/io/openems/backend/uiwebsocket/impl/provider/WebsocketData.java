package io.openems.backend.uiwebsocket.impl.provider;

import java.util.Optional;
import java.util.UUID;

public class WebsocketData {
	private final int userId;
	private final UUID uuid;
	private Optional<BackendCurrentDataWorker> currentDataWorker = Optional.empty();

	public WebsocketData(int userId, UUID uuid) {
		super();
		this.userId = userId;
		this.uuid = uuid;
	}

	public int getUserId() {
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
}
