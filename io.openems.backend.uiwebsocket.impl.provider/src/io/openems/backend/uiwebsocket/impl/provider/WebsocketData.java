package io.openems.backend.uiwebsocket.impl.provider;

import java.util.UUID;

public class WebsocketData {
	private final int userId;
	private final UUID uuid;

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
}
