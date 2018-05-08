package io.openems.edge.controller.api.websocket;

import java.util.UUID;

import org.java_websocket.WebSocket;

public class UiEdgeWebsocketHandler extends EdgeWebsocketHandler {

	private final String sessionToken;
	private final UUID uuid;

	public UiEdgeWebsocketHandler(WebsocketApi parent, WebSocket websocket, String sessionToken, UUID uuid) {
		super(parent, websocket);
		this.sessionToken = sessionToken;
		this.uuid = uuid;
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public UUID getUuid() {
		return uuid;
	}

}
