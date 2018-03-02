package io.openems.impl.controller.api.websocket;

import java.util.UUID;

import org.java_websocket.WebSocket;

import io.openems.core.utilities.api.ApiWorker;
import io.openems.core.utilities.websocket.EdgeWebsocketHandler;

public class UiEdgeWebsocketHandler extends EdgeWebsocketHandler {

	private final String sessionToken;
	private final UUID uuid;

	public UiEdgeWebsocketHandler(WebSocket websocket, ApiWorker apiWorker, String sessionToken, UUID uuid) {
		super(websocket, apiWorker);
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
