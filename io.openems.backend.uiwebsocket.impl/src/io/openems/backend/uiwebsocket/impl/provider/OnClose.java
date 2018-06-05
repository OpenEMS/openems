package io.openems.backend.uiwebsocket.impl.provider;

import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractOnClose;

public class OnClose extends AbstractOnClose {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);

	private final UiWebsocketServer parent;

	public OnClose(UiWebsocketServer parent, WebSocket websocket, int code, String reason, boolean remote) {
		super(websocket, code, reason, remote);
		this.parent = parent;
	}

	@Override
	protected void run(WebSocket websocket, int code, String reason, boolean remote) {
		// get current User
		WebsocketData data = websocket.getAttachment();
		log.info("User [" + this.parent.getUserName(data) + "] disconnected.");

		// stop CurrentDataWorker
		Optional<BackendCurrentDataWorker> currentDataWorkerOpt = data.getCurrentDataWorker();
		if (currentDataWorkerOpt.isPresent()) {
			currentDataWorkerOpt.get().dispose();
		}
		// remove websocket from local cache
		UUID uuid = data.getUuid();
		if (uuid != null) {
			synchronized (this.parent.websocketsMap) {
				this.parent.websocketsMap.remove(uuid);
			}
		}
	}
}
