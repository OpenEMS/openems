package io.openems.backend.b2bwebsocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);
	private final Backend2BackendWebsocket parent;

	public OnNotification(Backend2BackendWebsocket parent) {
		this.parent = parent;
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) throws OpenemsException {
		this.parent.logWarn(this.log, "Unhandled Notification: " + notification);
	}

}
