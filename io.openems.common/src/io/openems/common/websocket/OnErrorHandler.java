package io.openems.common.websocket;

import org.java_websocket.WebSocket;

public class OnErrorHandler implements Runnable {

	private final AbstractWebsocket<?> parent;
	private final WebSocket ws;
	private final Exception ex;

	public OnErrorHandler(AbstractWebsocket<?> parent, WebSocket ws, Exception ex) {
		this.parent = parent;
		this.ws = ws;
		this.ex = ex;
	}

	@Override
	public final void run() {
		try {
			this.parent.getOnError().run(this.ws, this.ex);

		} catch (Throwable t) {
			this.parent.handleInternalErrorSync(t, WebsocketUtils.getWsDataString(this.ws));
		}
	}

}
