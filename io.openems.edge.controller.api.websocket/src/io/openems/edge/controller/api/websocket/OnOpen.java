package io.openems.edge.controller.api.websocket;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.SecureRandomSingleton;
import io.openems.common.websocket_old.AbstractOnOpen;
import io.openems.common.websocket_old.LogBehaviour;
import io.openems.common.websocket_old.Notification;
import io.openems.common.websocket_old.WebSocketUtils;
import io.openems.edge.common.user.User;

public class OnOpen extends AbstractOnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final WebsocketApiServer parent;

	public OnOpen(WebsocketApiServer parent, WebSocket websocket, ClientHandshake handshake) {
		super(websocket, handshake);
		this.parent = parent;
	}

	@Override
	protected void run(WebSocket websocket, ClientHandshake handshake) {
		// generate UUID for this websocket (browser tab)
		UUID uuid = UUID.randomUUID();

		// get token from cookie or generate new token
		String token;
		Optional<String> cookieTokenOpt = AbstractOnOpen.getFieldFromHandshakeCookie(handshake, "token");
		if (cookieTokenOpt.isPresent()) {
			token = cookieTokenOpt.get();
		} else {
			// Generate token (source: http://stackoverflow.com/a/41156)
			SecureRandom sr = SecureRandomSingleton.getInstance();
			token = new BigInteger(WebsocketApiServer.TOKEN_LENGTH, sr).toString(32);
		}

		// create new Handler and store it
		UiEdgeWebsocketHandler handler = new UiEdgeWebsocketHandler(this.parent.parent, websocket, token, uuid);
		this.parent.handlers.put(uuid, handler);
		websocket.setAttachment(uuid);

		// login using token from the cookie
		if (cookieTokenOpt.isPresent()) {
			User user = this.parent.sessionTokens.get(token);
			if (user != null) {
				/*
				 * token from cookie is valid -> authentication successful
				 */
				// send reply and log
				try {
					this.parent.handleAuthenticationSuccessful(handler, user);
					this.parent.parent.logInfo(this.log, "User [" + user.getName() + "] logged in by token");
					return;
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
							LogBehaviour.WRITE_TO_LOG, Notification.ERROR, e.getMessage());
				}
			}
		}

		// if we are here, automatic authentication was not possible -> notify client
		WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /* empty message id */,
				LogBehaviour.WRITE_TO_LOG, Notification.EDGE_AUTHENTICATION_BY_TOKEN_FAILED, cookieTokenOpt.orElse(""));

	}
}
