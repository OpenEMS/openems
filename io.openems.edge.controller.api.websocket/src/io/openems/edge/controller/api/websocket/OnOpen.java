package io.openems.edge.controller.api.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.OpenemsConstants;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdNotification;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdNotification.EdgeMetadata;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.user.User;

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
		Optional<String> cookieToken = JsonUtils.getAsOptionalString(handshake, "token");
		if (cookieToken.isPresent()) {
			try {
				// read token from Cookie
				token = UUID.fromString(cookieToken.get());

				// login using token from the cookie
				User user = this.parent.sessionTokens.get(token);
				if (user != null) {
					/*
					 * token from cookie is valid -> authentication successful
					 */
					// store user in attachment
					wsData.setUser(user);

					// send authentication notification
					List<EdgeMetadata> metadatas = new ArrayList<>();
					metadatas.add(new EdgeMetadata("edge0", "", "", OpenemsConstants.VERSION, Role.GUEST, true));
					AuthenticateWithSessionIdNotification notification = new AuthenticateWithSessionIdNotification(
							token, metadatas);
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

		// if we are here, automatic authentication was not possible -> notify client
// TODO
		// WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject() /*
		// empty message id */,
//				LogBehaviour.WRITE_TO_LOG, Notification.EDGE_AUTHENTICATION_BY_TOKEN_FAILED, cookieTokenOpt.orElse(""));
	}

}
