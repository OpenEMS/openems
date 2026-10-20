package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;

public class OnClose implements io.openems.common.websocket.OnClose {

	private final UiWebsocketImpl parent;

	public OnClose(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void accept(WebSocket ws, int code, String reason, boolean remote) {
		WsData wsData = ws.getAttachment();
		if (wsData == null) {
			return;
		}
		synchronized (wsData.updateSubscriptionsLock) {
			this.parent.removeEdgeSubscriptions(wsData);
			wsData.clearSubscribedEdges();
		}
		wsData.dispose();
	}

}