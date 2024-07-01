package io.openems.edge.controller.api.websocket;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	public OnOpen(ControllerApiWebsocket parent) {
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) {
	}
}
