package io.openems.backend.uiwebsocket.impl.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.AbstractOnClose;
import io.openems.common.websocket.AbstractOnError;
import io.openems.common.websocket.AbstractOnMessage;
import io.openems.common.websocket.AbstractOnOpen;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;

public class UiWebsocketServer extends AbstractWebsocketServer {

	protected final UiWebsocket parent;
	protected final Map<UUID, WebSocket> websocketsMap = new HashMap<>();

	public UiWebsocketServer(UiWebsocket parent, int port) {
		super(port);
		this.parent = parent;
	}

	protected void handleEdgeReply(int edgeId, JsonObject jMessage) throws OpenemsException {
		JsonObject jMessageId = JsonUtils.getAsJsonObject(jMessage, "messageId");
		String backendId = JsonUtils.getAsString(jMessageId, "backend");
		WebSocket websocket = this.websocketsMap.get(UUID.fromString(backendId));
		if (websocket != null) {
			JsonObject j = DefaultMessages.prepareMessageForForwardToUi(jMessage);
			WebSocketUtils.send(websocket, j);
			return;
		}
		throw new OpenemsException("No websocket found for UUID [" + backendId + "]");
	}

	protected String getUserName(WebsocketData data) {
		Integer userId = data.getUserId();
		if (userId == null) {
			return "ID:UNKNOWN";
		}
		Optional<User> userOpt = this.parent.metadataService.getUser(userId);
		if (userOpt.isPresent()) {
			return userOpt.get().getName();
		}
		return "ID:" + data.getUserId();
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
