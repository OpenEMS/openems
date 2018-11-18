package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

public class OnOpenHandler implements Runnable {

	private final AbstractWebsocket parent;
	private final WebSocket ws;
	private final JsonObject handshake;

	public OnOpenHandler(AbstractWebsocket parent, WebSocket ws, JsonObject handshake) {
		this.parent = parent;
		this.ws = ws;
		this.handshake = handshake;
	}

	@Override
	public final void run() {
		try {
			this.parent.getOnOpen().run(this.ws, this.handshake);
		} catch (Exception e) {
			this.parent.handleInternalErrorSync(e);
		}
	}

}
