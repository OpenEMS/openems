package io.openems.backend.b2bwebsocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.websocket.WebSocket;

public class OnError implements io.openems.common.websocket.OnError {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);
	private final Backend2BackendWebsocket parent;

	public OnError(Backend2BackendWebsocket parent) {
		this.parent = parent;
	}

	@Override
	public void accept(WebSocket ws, Exception ex) throws OpenemsException {
		this.parent.logInfo(this.log, "Error: " + ex.getMessage());
	}

}
