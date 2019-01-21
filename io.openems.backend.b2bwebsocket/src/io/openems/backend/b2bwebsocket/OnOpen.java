package io.openems.backend.b2bwebsocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);
	private final B2bWebsocket parent;

	public OnOpen(B2bWebsocket parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) throws OpenemsException {
		this.parent.logInfo(this.log, "Opened");
	}

}
