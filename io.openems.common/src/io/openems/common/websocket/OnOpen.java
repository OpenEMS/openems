package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;

@FunctionalInterface
public interface OnOpen {

	public void run(WebSocket ws, JsonObject handshake) throws OpenemsException;

}
