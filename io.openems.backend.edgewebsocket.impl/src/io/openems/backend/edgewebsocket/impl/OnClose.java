package io.openems.backend.edgewebsocket.impl;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsException;

public class OnClose implements io.openems.common.websocket.OnClose {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);
	private final EdgeWebsocketImpl parent;

	public OnClose(EdgeWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, int code, String reason, boolean remote) throws OpenemsException {
		// get edgeId from websocket
		WsData wsData = ws.getAttachment();
		Optional<String> edgeIdOpt = wsData.getEdgeId();
		if (!edgeIdOpt.isPresent()) {
			return;
		}

		// if there is no other websocket connection for this edgeId -> announce Edge as
		// offline
		String edgeId = edgeIdOpt.get();
		Optional<Edge> edgeOpt = this.parent.metadata.getEdge(edgeId);
		if (edgeOpt.isPresent()) {
			boolean isOnline = this.parent.isOnline(edgeId);
			edgeOpt.get().setOnline(isOnline);
		}

		// TODO send notification, to UI
		
		// log
		this.parent.logInfo(this.log, "Edge [" + edgeId + "] disconnected.");
	}

}
