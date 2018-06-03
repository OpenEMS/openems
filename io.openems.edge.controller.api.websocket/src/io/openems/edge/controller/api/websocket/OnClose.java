package io.openems.edge.controller.api.websocket;

import java.util.UUID;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.websocket.AbstractOnClose;

public class OnClose extends AbstractOnClose {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);

	private final WebsocketApiServer parent;

	public OnClose(WebsocketApiServer parent, WebSocket websocket, int code, String reason, boolean remote) {
		super(websocket, code, reason, remote);
		this.parent = parent;
	}

	@Override
	protected void run(WebSocket websocket, int code, String reason, boolean remote) {
		this.parent.parent.logInfo(this.log,
				"User [" + this.parent.getUserName(websocket) + "] closed websocket connection");
		this.disposeHandler(websocket);
	}

	private void disposeHandler(WebSocket websocket) {
		UiEdgeWebsocketHandler handler;
		try {
			handler = this.parent.getHandlerOrCloseWebsocket(websocket);
			UUID uuid = handler.getUuid();
			this.parent.handlers.remove(uuid);
			handler.dispose();
		} catch (OpenemsException e) {
			this.parent.parent.log.warn("Unable to dispose Handler: " + e.getMessage());
		}
	}

}
