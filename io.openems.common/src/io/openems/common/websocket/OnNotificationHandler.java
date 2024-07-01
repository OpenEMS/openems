package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.jsonrpc.base.JsonrpcNotification;

public class OnNotificationHandler implements Runnable {

	private final AbstractWebsocket<?> parent;
	private final WebSocket ws;
	private final JsonrpcNotification notification;

	public OnNotificationHandler(AbstractWebsocket<?> parent, WebSocket ws, JsonrpcNotification notification) {
		this.parent = parent;
		this.ws = ws;
		this.notification = notification;
	}

	@Override
	public final void run() {
		try {
			this.parent.getOnNotification().run(this.ws, this.notification);

		} catch (Throwable t) {
			this.parent.handleInternalErrorSync(t, WebsocketUtils.getWsDataString(this.ws));
		}
	}

}
