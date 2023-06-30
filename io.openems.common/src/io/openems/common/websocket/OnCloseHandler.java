package io.openems.common.websocket;

import org.java_websocket.WebSocket;

public class OnCloseHandler implements Runnable {

	private final AbstractWebsocket<?> parent;
	private final WebSocket ws;
	private final int code;
	private final String reason;
	private final boolean remote;

	public OnCloseHandler(AbstractWebsocket<?> parent, WebSocket ws, int code, String reason, boolean remote) {
		this.parent = parent;
		this.ws = ws;
		this.code = code;
		this.reason = reason;
		this.remote = remote;
	}

	@Override
	public final void run() {
		try {
			this.parent.getOnClose().run(this.ws, this.code, this.reason, this.remote);

			// remove websocket from WsData
			WsData wsData = this.ws.getAttachment();
			if (wsData != null) {
				wsData.setWebsocket(null);
			}

		} catch (Throwable t) {
			this.parent.handleInternalErrorSync(t, WebsocketUtils.getWsDataString(this.ws));
		}
	}

}
