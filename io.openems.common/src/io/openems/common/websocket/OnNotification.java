package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

@FunctionalInterface
public interface OnNotification {

	public void run(WebSocket ws, JsonrpcNotification notification) throws OpenemsException;

}
