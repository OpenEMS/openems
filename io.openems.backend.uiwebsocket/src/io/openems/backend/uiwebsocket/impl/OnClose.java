package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;

public class OnClose implements io.openems.common.websocket.OnClose {

	public OnClose(UiWebsocketImpl parent) {
	}

	@Override
	public void run(WebSocket ws, int code, String reason, boolean remote) throws OpenemsException {
		// get current User
		WsData wsData = ws.getAttachment();
		if (wsData != null) {
			wsData.dispose();
		}
	}

}
