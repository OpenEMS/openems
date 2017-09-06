package io.openems.impl.controller.api.websocket.session;

import io.openems.api.security.User;
import io.openems.common.session.SessionData;

public class WebsocketApiSessionData extends SessionData {
	private final User user;

	public WebsocketApiSessionData(User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	public String getRole() {
		return this.user.getName();
	}
}
