package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;

public class OnClose implements io.openems.common.websocket.OnClose {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);
	private final UiWebsocketImpl parent;

	public OnClose(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, int code, String reason, boolean remote) throws OpenemsException {
		// get current User
		WsData wsData = ws.getAttachment();
		var userOpt = wsData.getUser(this.parent.metadata);
		if (userOpt.isPresent()) {
			var user = userOpt.get();
			this.parent.logInfo(this.log, "User [" + user.getId() + ":" + user.getName() + "] disconnected.");
		} else {
			this.parent.logInfo(this.log, "User [" + wsData.getUserId().orElse("UNKNOWN") + "] disconnected.");
		}

		wsData.dispose();
	}

}
