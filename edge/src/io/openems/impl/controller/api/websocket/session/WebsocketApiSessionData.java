package io.openems.impl.controller.api.websocket.session;

import com.google.gson.JsonObject;

import io.openems.api.security.User;
import io.openems.common.session.SessionData;
import io.openems.core.utilities.websocket.EdgeWebsocketHandler;

public class WebsocketApiSessionData extends SessionData {
	private final User user;
	private final EdgeWebsocketHandler websocketHandler;

	public WebsocketApiSessionData(User user, EdgeWebsocketHandler websocketHandler) {
		this.user = user;
		this.websocketHandler = websocketHandler;
	}

	public User getUser() {
		return user;
	}

	public String getRole() {
		return this.user.getName();
	}

	public EdgeWebsocketHandler getWebsocketHandler() {
		return websocketHandler;
	}

	@Override
	public String toString() {
		return "WebsocketApiSessionData [user=" + user + "]";
	}

	@Override
	public JsonObject toJsonObject() {
		JsonObject j = new JsonObject();
		// TODO Auto-generated method stub
		return j;
	}
}
