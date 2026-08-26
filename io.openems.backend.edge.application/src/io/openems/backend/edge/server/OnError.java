package io.openems.backend.edge.server;

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
		WsData wsData = ws.getAttachment();
		this.logError.accept(this.log, new StringBuilder() //
				.append("[").append(wsData.getEdgeIdString()) //
				.append("] Websocket error. ").append(ex.getClass().getSimpleName()).append(": ") //
				.append(ex.getMessage()) //
				.toString());
		ex.printStackTrace();
	}

}
