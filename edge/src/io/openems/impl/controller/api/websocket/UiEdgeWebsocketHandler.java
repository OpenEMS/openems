package io.openems.impl.controller.api.websocket;

import io.openems.api.security.User;
import io.openems.core.utilities.websocket.EdgeWebsocketHandler;

public class UiEdgeWebsocketHandler extends EdgeWebsocketHandler {

	private final String token;
	private final User user;

	public UiEdgeWebsocketHandler(String token, User user) {
		this.token = token;
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public User getUser() {
		return user;
	}
}
