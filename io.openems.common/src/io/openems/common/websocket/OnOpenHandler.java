package io.openems.common.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class OnOpenHandler implements Runnable {

	private final Logger log = LoggerFactory.getLogger(OnOpenHandler.class);
	private final AbstractWebsocket<?> parent;
	private final WebSocket ws;
	private final JsonObject handshake;

	public OnOpenHandler(AbstractWebsocket<?> parent, WebSocket ws, JsonObject handshake) {
		this.parent = parent;
		this.ws = ws;
		this.handshake = handshake;
	}

	@Override
	public final void run() {
		try {
			this.parent.getOnOpen().run(this.ws, this.handshake);

		} catch (WebsocketNotConnectedException e) {
			this.parent.logWarn(this.log,
					"Websocket was closed before it has been fully opened: " + WebsocketUtils.getWsDataString(this.ws));

		} catch (Throwable t) {
			this.parent.handleInternalErrorSync(t, WebsocketUtils.getWsDataString(this.ws));
		}
	}

}
