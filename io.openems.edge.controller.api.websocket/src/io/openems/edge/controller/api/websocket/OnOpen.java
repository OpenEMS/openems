package io.openems.edge.controller.api.websocket;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import io.openems.common.access_control.RoleId;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdFailedNotification;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdNotification;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final WebsocketApi parent;

	OnOpen(WebsocketApi parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) {
		// get websocket attachment
		WsData wsData = ws.getAttachment();

		// get token from cookie or generate new token
		Optional<String> cookieTokenOpt = io.openems.common.websocket.OnOpen.getFieldFromHandshakeCookie(handshake,
				"token");

		// get token from cookie or generate new token
		AtomicReference<UUID> token = new AtomicReference<>();
		cookieTokenOpt.ifPresent(cookieToken -> {
			try {
				// read token from Cookie
				token.set(UUID.fromString(cookieToken));

				// login using token from the cookie
				RoleId roleId = this.parent.sessionTokensNew.get(token.get());
				if (roleId != null) {
					/*
					 * token from cookie is valid -> authentication successful
					 */
					// store user in attachment

					wsData.setRoleId(roleId);
					// send authentication notification
					AuthenticateWithSessionIdNotification notification = new AuthenticateWithSessionIdNotification(
							token.get(), Utils.getEdgeMetadata(wsData.getRoleId()));
					this.parent.server.sendMessage(ws, notification);

					// log
					this.parent.logInfo(this.log, "Role [" + roleId + "] logged in by token");
				}

			} catch (IllegalArgumentException e) {
				this.parent.logWarn(this.log, "Cookie Token [" + token + "] is not a UUID: " + e.getMessage());
			}
		});

		if (token.get() == null) {
			token.set(UUID.randomUUID());
		}
		wsData.setSessionToken(token.get());

		if (wsData.getRoleId() == null) {
			// automatic authentication was not possible -> notify client
			this.parent.server.sendMessage(ws, new AuthenticateWithSessionIdFailedNotification());
		}
	}

}
