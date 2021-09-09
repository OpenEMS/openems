package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	public OnOpen(UiWebsocketImpl parent) {
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) throws OpenemsException {
	}
}
