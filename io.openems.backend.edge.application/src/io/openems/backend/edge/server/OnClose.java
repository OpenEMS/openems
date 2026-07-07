package io.openems.backend.edge.server;

import org.java_websocket.WebSocket;

public class OnClose implements io.openems.common.websocket.OnClose {

	private final Runnable connectedEdgesChanged;

	public OnClose(//
			Runnable connectedEdgesChanged) {
		this.connectedEdgesChanged = connectedEdgesChanged;
	}

	@Override
	public void accept(WebSocket ws, int code, String reason, boolean remote) {
		this.connectedEdgesChanged.run();
	}

}
