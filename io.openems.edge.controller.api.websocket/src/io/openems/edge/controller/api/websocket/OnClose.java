package io.openems.edge.controller.api.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnClose implements io.openems.common.websocket.OnClose {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);
	private final ControllerApiWebsocketImpl parent;

	public OnClose(ControllerApiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void accept(WebSocket ws, int code, String reason, boolean remote) {
		// get websocket attachment
		WsData wsData = ws.getAttachment();
		var user = wsData.getUser();

		// print log message
		String logMessage;
		if (user.isPresent()) {
			logMessage = "User [" + user.get() + "] closed websocket connection.";
		} else {
			logMessage = "Unknown User [" + wsData.getSessionToken() + "] closed websocket connection.";
		}
		this.parent.logInfo(this.log, logMessage);
	}

}
