package io.openems.backend.b2bwebsocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;

public class OnClose implements io.openems.common.websocket.OnClose {

	@Override
	public void run(WebSocket ws, int code, String reason, boolean remote) throws OpenemsException {
	}

}
