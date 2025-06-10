package io.openems.edge.edge2edge.websocket.bridge;

import java.net.ConnectException;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;

public class OnError implements io.openems.common.websocket.OnError {

	private final Logger log = LoggerFactory.getLogger(OnError.class);

	@Override
	public void accept(WebSocket ws, Exception ex) throws OpenemsException {
		if (ex instanceof ConnectException) {
			this.log.error("OnError: " + ex.getMessage());
			return;
		}
		this.log.error("OnError", ex);
	}

}
