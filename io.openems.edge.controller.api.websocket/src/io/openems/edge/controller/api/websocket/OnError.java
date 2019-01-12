package io.openems.edge.controller.api.websocket;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.user.User;

public class OnError implements io.openems.common.websocket.OnError {

	private final Logger log = LoggerFactory.getLogger(OnError.class);
	private final WebsocketApi parent;

	public OnError(WebsocketApi parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, Exception ex) throws OpenemsException {
		// get websocket attachment
		WsData wsData = ws.getAttachment();
		Optional<User> user = wsData.getUser();

		String logMessage;
		if (user.isPresent()) {
			logMessage = "User [" + user.get().getName() + "] error: ";
		} else {
			logMessage = "Unknown User [" + wsData.getSessionToken() + "] error: ";
		}

		this.parent.logWarn(this.log, logMessage + ex.getMessage());
	}

}
