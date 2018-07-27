package io.openems.backend.uiwebsocket.energydepot;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractOnError;

public class OnError extends AbstractOnError {

	private final Logger log = LoggerFactory.getLogger(OnError.class);
	private final UiWebsocketServer parent;

	public OnError(UiWebsocketServer parent, WebSocket websocket, Exception ex) {
		super(websocket, ex);
		this.parent = parent;
	}

	@Override
	protected void run(WebSocket websocket, Exception ex) {
		WebsocketData data = websocket.getAttachment();
		log.warn("User [" + this.parent.getUserName(data) + "] websocket error. " + ex.getClass().getSimpleName() + ": "
				+ ex.getMessage());
	}

}
