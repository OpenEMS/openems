package io.openems.backend.edge.manager;

import java.util.function.BiConsumer;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;

public class OnError implements io.openems.common.websocket.OnError {

	private final Logger log = LoggerFactory.getLogger(OnError.class);
	private final BiConsumer<Logger, String> logError;

	public OnError(BiConsumer<Logger, String> logError) {
		this.logError = logError;
	}

	@Override
	public void accept(WebSocket ws, Exception ex) throws OpenemsException {
		this.logError.accept(this.log, "Websocket error. " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
		this.log.error(ex.getMessage(), ex);
	}

}
