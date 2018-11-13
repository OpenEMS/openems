package io.openems.backend.edgewebsocket.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.websocket_old.AbstractOnClose;
import io.openems.common.websocket_old.AbstractOnError;
import io.openems.common.websocket_old.AbstractOnMessage;
import io.openems.common.websocket_old.AbstractOnOpen;
import io.openems.common.websocket_old.AbstractWebsocketServer;
import io.openems.common.websocket_old.WebSocketUtils;

public class EdgeWebsocketServer extends AbstractWebsocketServer {

	protected final EdgeWebsocket parent;
	protected final Map<Integer, WebSocket> websocketsMap = new HashMap<>();

	public EdgeWebsocketServer(EdgeWebsocket parent, int port) {
		super(port);
		this.parent = parent;
	}

	public boolean isOnline(int edgeId) {
		return this.websocketsMap.containsKey(edgeId);
	}

	protected String[] getEdgeNames(int[] edgeIds) {
		String[] edgeNames = new String[edgeIds.length];
		for (int i = 0; i < edgeIds.length; i++) {
			Optional<Edge> edgeOpt = this.parent.metadataService.getEdgeOpt(edgeIds[i]);
			if (edgeOpt.isPresent()) {
				edgeNames[i] = edgeOpt.get().getName();
			} else {
				edgeNames[i] = "ID:" + edgeIds[i];
			}
		}
		return edgeNames;
	}

	public void forwardMessageFromUi(int edgeId, JsonObject jMessage) throws OpenemsException {
		WebSocket websocket = this.websocketsMap.get(edgeId);
		if (websocket != null) {
			WebSocketUtils.send(websocket, jMessage);
		}
	}

	@Override
	protected AbstractOnMessage _onMessage(WebSocket websocket, String message) {
		return new OnMessage(this, websocket, message);
	}

	@Override
	protected AbstractOnOpen _onOpen(WebSocket websocket, ClientHandshake handshake) {
		return new OnOpen(this, websocket, handshake);
	}

	@Override
	protected AbstractOnError _onError(WebSocket websocket, Exception ex) {
		return new OnError(this, websocket, ex);
	}

	@Override
	protected AbstractOnClose _onClose(WebSocket websocket, int code, String reason, boolean remote) {
		return new OnClose(this, websocket, code, reason, remote);
	}
}
