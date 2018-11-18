package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metadata.api.Edge;
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
		Optional<String> userId = wsData.getUserId();
		log.info("User [" + userId.orElse("UNKNOWN") + "] disconnected.");

		// stop CurrentDataWorker
		Optional<BackendCurrentDataWorker> currentDataWorkerOpt = wsData.getCurrentDataWorker();
		if (currentDataWorkerOpt.isPresent()) {
			currentDataWorkerOpt.get().dispose();
		}
	}

}
