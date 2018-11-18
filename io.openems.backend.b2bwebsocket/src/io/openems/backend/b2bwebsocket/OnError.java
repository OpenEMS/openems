package io.openems.backend.b2bwebsocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;

public class OnError implements io.openems.common.websocket.OnError {

	@Override
	public void run(WebSocket ws, Exception ex) throws OpenemsException {
	}

}
