package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;

public class OnError implements io.openems.common.websocket.OnError {

	private final Logger log;

	public OnError(UiWebsocketImpl parent) {
		this.log = AbstractOpenemsBackendComponent.getComponentLogger(this.getClass(), parent);
	}

	@Override
	public void accept(WebSocket ws, Exception ex) {
		WsData wsData = ws.getAttachment();
		final var userId = wsData.getUserId().orElse("UNKNOWN");
		this.log.warn("User [{}] websocket error. {}: {}", //
				userId, ex.getClass().getSimpleName(), ex.getMessage());
	}

}
