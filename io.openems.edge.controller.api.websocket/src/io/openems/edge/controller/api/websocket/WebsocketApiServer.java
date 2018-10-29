package io.openems.edge.controller.api.websocket;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.websocket.AbstractOnClose;
import io.openems.common.websocket.AbstractOnError;
import io.openems.common.websocket.AbstractOnMessage;
import io.openems.common.websocket.AbstractOnOpen;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.DefaultMessages;
import io.openems.edge.common.user.User;

final class WebsocketApiServer extends AbstractWebsocketServer {

	protected final static int TOKEN_LENGTH = 130;

	protected final WebsocketApi parent;

	/**
	 * Stores valid session tokens for authentication via Cookie (this maps to a
	 * browser window)
	 */
	protected final Map<String, User> sessionTokens = new ConcurrentHashMap<>();
	/**
	 * Stores handlers per websocket (this maps to a browser tab). The handler lives
	 * while the websocket is connected. Independently of the login/logout state.
	 */
	protected final Map<UUID, UiEdgeWebsocketHandler> handlers = new ConcurrentHashMap<>();

	WebsocketApiServer(WebsocketApi parent, int port) {
		super(port);
		this.parent = parent;
	}

	protected void handleAuthenticationSuccessful(UiEdgeWebsocketHandler handler, User user) throws OpenemsException {
		// add user to handler
		handler.setUser(user);

		// Create Edges entry
		JsonObject jEdge = new JsonObject();
		jEdge.addProperty("id", 0);
		jEdge.addProperty("name", "fems0");
		jEdge.addProperty("comment", "FEMS");
		jEdge.addProperty("producttype", "");
		jEdge.addProperty("version", OpenemsConstants.VERSION);
		jEdge.add("role", user.getRole().asJson());
		jEdge.addProperty("online", true);
		JsonArray jEdges = new JsonArray();
		jEdges.add(jEdge);

		// send reply
		JsonObject jReply = DefaultMessages.uiLoginSuccessfulReply(handler.getSessionToken(), jEdges);
		handler.send(jReply);
	}

	protected void sendLog(PaxLoggingEvent event) {
		for (UiEdgeWebsocketHandler handler : this.handlers.values()) {
			handler.sendLog(event);
		}
	}

	protected String getUserName(WebSocket websocket) {
		Optional<UiEdgeWebsocketHandler> handlerOpt = getHandlerOpt(websocket);
		if (handlerOpt.isPresent()) {
			UiEdgeWebsocketHandler handler = handlerOpt.get();
			Optional<User> userOpt = handler.getUserOpt();
			if (userOpt.isPresent()) {
				User user = userOpt.get();
				return user.getName();
			}
		}
		return "UNKNOWN";
	}

	protected Optional<UiEdgeWebsocketHandler> getHandlerOpt(WebSocket websocket) {
		UUID uuid = websocket.getAttachment();
		return Optional.ofNullable(this.handlers.get(uuid));
	}

	protected UiEdgeWebsocketHandler getHandlerOrCloseWebsocket(WebSocket websocket) throws OpenemsException {
		Optional<UiEdgeWebsocketHandler> handlerOpt = this.getHandlerOpt(websocket);
		UUID uuid = websocket.getAttachment();
		UiEdgeWebsocketHandler handler = this.handlers.get(uuid);
		if (!handlerOpt.isPresent()) {
			// no handler! close websocket
			websocket.close();
			throw new OpenemsException("Websocket had no Handler. Closing websocket.");
		}
		return handler;
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