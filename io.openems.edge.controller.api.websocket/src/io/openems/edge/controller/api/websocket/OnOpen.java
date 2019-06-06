package io.openems.edge.controller.api.websocket;

import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdFailedNotification;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdNotification;
import io.openems.edge.common.user.EdgeUser;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final WebsocketApi parent;

	public OnOpen(WebsocketApi parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) {
		// get websocket attachment
		WsData wsData = ws.getAttachment();

		// get token from cookie or generate new token
		UUID token = null;
		Optional<String> cookieToken = io.openems.common.websocket.OnOpen.getFieldFromHandshakeCookie(handshake,
				"token");
		if (cookieToken.isPresent()) {
			try {
				// read token from Cookie
				token = UUID.fromString(cookieToken.get());

				// login using token from the cookie
				EdgeUser user = this.parent.sessionTokens.get(token);
				if (user != null) {
					/*
					 * token from cookie is valid -> authentication successful
					 */
					// store user in attachment
					wsData.setUser(user);

					// send authentication notification
					AuthenticateWithSessionIdNotification notification = new AuthenticateWithSessionIdNotification(
							token, Utils.getEdgeMetadata(user.getRole()));
					this.parent.server.sendMessage(ws, notification);

					// log
					this.parent.logInfo(this.log, "User [" + user.getName() + "] logged in by token");
				}

			} catch (IllegalArgumentException e) {
				this.parent.logWarn(this.log, "Cookie Token [" + token + "] is not a UUID: " + e.getMessage());
			}
		}
		if (token == null) {
			token = UUID.randomUUID();
		}
		wsData.setSessionToken(token);

		if (!wsData.getUser().isPresent()) {
			// automatic authentication was not possible -> notify client
			this.parent.server.sendMessage(ws, new AuthenticateWithSessionIdFailedNotification());
		}
	}

}
