package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;

@FunctionalInterface
public interface OnError {

	public void run(WebSocket ws, Exception ex) throws OpenemsException;

}
