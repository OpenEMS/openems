package io.openems.common.websocket;

import org.java_websocket.WebSocket;

import io.openems.common.jsonrpc.base.JsonrpcResponse;

public class OnResponseHandler implements Runnable {

	private final AbstractWebsocket<?> parent;
	private final WebSocket ws;
	private final JsonrpcResponse response;

	public OnResponseHandler(AbstractWebsocket<?> parent, WebSocket ws, JsonrpcResponse response) {
		this.parent = parent;
		this.ws = ws;
		this.response = response;
	}

	@Override
	public final void run() {
		try {
			WsData wsData = this.ws.getAttachment();
			wsData.handleJsonrpcResponse(this.response);

		} catch (Throwable t) {
			this.parent.handleInternalErrorSync(t, WebsocketUtils.getWsDataString(this.ws));
		}
	}

}
