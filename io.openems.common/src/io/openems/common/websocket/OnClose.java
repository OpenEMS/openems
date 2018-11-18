package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;

@FunctionalInterface
public interface OnClose {

	public void run(WebSocket ws, int code, String reason, boolean remote) throws OpenemsException;

}
