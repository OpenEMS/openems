package io.openems.edge.controller.api.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;

public class OnError implements io.openems.common.websocket.OnError {

	private final Logger log = LoggerFactory.getLogger(OnError.class);
	private final WebsocketApiImpl parent;

	public OnError(WebsocketApiImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, Exception ex) throws OpenemsException {
		// get websocket attachment
		WsData wsData = ws.getAttachment();
		var user = wsData.getUser();

		String logMessage;
		if (user.isPresent()) {
			logMessage = "User [" + user.get().getName() + "] error: ";
		} else {
			logMessage = "Unknown User [" + wsData.getSessionToken() + "] error: ";
		}

		this.parent.logWarn(this.log, logMessage + ex.getMessage());
	}

}
