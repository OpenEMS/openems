package io.openems.edge.controller.api.backend;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;

public class OnError implements io.openems.common.websocket.OnError {

	private final Logger log = LoggerFactory.getLogger(OnError.class);
	private final ControllerApiBackendImpl parent;

	public OnError(ControllerApiBackendImpl parent) {
		this.parent = parent;
	}

	@Override
	public void accept(WebSocket ws, Exception ex) throws OpenemsException {
		this.parent.logWarn(this.log, "Error: " + ex.getMessage());
	}

}
