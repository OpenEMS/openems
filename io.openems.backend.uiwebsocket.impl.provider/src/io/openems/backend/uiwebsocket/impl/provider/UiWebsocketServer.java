package io.openems.backend.uiwebsocket.impl.provider;

import java.util.Map.Entry;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Role;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;

public class UiWebsocketServer extends AbstractWebsocketServer {

	private final Logger log = LoggerFactory.getLogger(UiWebsocketServer.class);
	private final UiWebsocket parent;

	public UiWebsocketServer(UiWebsocket parent, int port) {
		super(port);
		this.parent = parent;
	}

	@Override
	protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
		String error = "";
		User user = null;

		// login using session_id from the cookie
		Optional<String> sessionIdOpt = getSessionIdFromHandshake(handshake);
		if (!sessionIdOpt.isPresent()) {
			error = "Session-ID is missing in handshake";
		} else {
			try {
				user = this.parent.metadataService.getUserWithSession(sessionIdOpt.get());
			} catch (OpenemsException e) {
				error = e.getMessage();
			}
		}

		if (!error.isEmpty()) {
			// send connection failed to browser
			this.send(websocket, DefaultMessages.browserConnectionFailedReply());
			log.warn("User connection failed. Session [" + sessionIdOpt.orElse("") + "] Error [" + error + "].");
			websocket.closeConnection(CloseFrame.REFUSE, error);
			
		} else if (user != null) {
			// send connection successful to browser
			JsonArray jEdges = new JsonArray();
			for (Entry<Integer, Role> edgeRole : user.getEdgeRoles().entrySet()) {
				int edgeId = edgeRole.getKey();
				Role role = edgeRole.getValue();
				Optional<Edge> edgeOpt = this.parent.metadataService.getEdge(edgeId);
				if (!edgeOpt.isPresent()) {
					log.warn("Unable to find Edge [ID:" + edgeId + "]");
				} else {
					JsonObject jEdge = edgeOpt.get().toJsonObject();
					jEdge.addProperty("role", role.toString());
					jEdges.add(jEdge);
				}
			}
			JsonObject jReply = DefaultMessages.browserConnectionSuccessfulReply("" /* TODO empty token? */,
					Optional.empty(), jEdges);
			WebSocketUtils.send(websocket, jReply);
			log.info("User [" + user.getName() + "] connected with Session [" + sessionIdOpt.orElse("") + "].");
		}
	}

	@Override
	protected void _onMessage(WebSocket websocket, JsonObject jMessage, Optional<JsonArray> jMessageIdOpt,
			Optional<String> deviceNameOpt) {
		log.info("UiWebsocketServer: On Message");
	}

	@Override
	protected void _onError(WebSocket websocket, Exception ex) {
		log.info("UiWebsocketServer: On Error");
	}

	@Override
	protected void _onClose(WebSocket websocket) {
		log.info("UiWebsocketServer: On Close");
	}
}
