package io.openems.backend.edgewebsocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;

public class OnError implements io.openems.common.websocket.OnError {

	private final Logger log = LoggerFactory.getLogger(OnError.class);
	private final EdgeWebsocketImpl parent;

	public OnError(EdgeWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, Exception ex) throws OpenemsException {
		WsData wsData = ws.getAttachment();
		var edgeIdOpt = wsData.getEdgeId();
		this.parent.logWarn(this.log, edgeIdOpt.orElse(null),
				"Websocket error. " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
		ex.printStackTrace();
	}

}
