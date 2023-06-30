package io.openems.backend.b2bwebsocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;

public class OnClose implements io.openems.common.websocket.OnClose {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);
	private final Backend2BackendWebsocket parent;

	public OnClose(Backend2BackendWebsocket parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, int code, String reason, boolean remote) throws OpenemsException {
		WsData wsData = ws.getAttachment();
		var user = wsData.getUserOpt();
		if (user.isPresent()) {
			this.parent.logInfo(this.log, "User [" + user.get().getName() + "] closed connection");
		} else {
			this.parent.logInfo(this.log, "Connection closed");
		}
	}

}
