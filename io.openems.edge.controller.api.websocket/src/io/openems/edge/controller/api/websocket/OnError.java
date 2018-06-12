package io.openems.edge.controller.api.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractOnError;

public class OnError extends AbstractOnError {

	private final Logger log = LoggerFactory.getLogger(OnError.class);
	private final WebsocketApiServer parent;

	public OnError(WebsocketApiServer parent, WebSocket websocket, Exception ex) {
		super(websocket, ex);
		this.parent = parent;
	}

	@Override
	protected void run(WebSocket websocket, Exception ex) {
		this.parent.parent.logWarn(this.log,
				"User [" + this.parent.getUserName(websocket) + "] error: " + ex.getMessage());
	}

}
