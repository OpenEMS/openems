package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdFailedNotification;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdNotification;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final UiWebsocketImpl parent;

	public OnOpen(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) throws OpenemsException {
		// get websocket attachment
		WsData wsData = ws.getAttachment();

		// declare user
		BackendUser user;

		// login using session_id from the handshake
		Optional<String> sessionIdOpt = io.openems.common.websocket.OnOpen.getFieldFromHandshakeCookie(handshake,
				"session_id");
		try {
			if (sessionIdOpt.isPresent()) {
				// authenticate with Session-ID
				user = this.parent.metadata.authenticate(sessionIdOpt.get());
			} else {
				Optional<String> tokenOpt = io.openems.common.websocket.OnOpen.getFieldFromHandshakeCookie(handshake,
						"token");
				if (tokenOpt.isPresent()) {
					// authenticate with Token
					user = this.parent.metadata.authenticate(tokenOpt.get());
				} else {
					// authenticate without Session-ID
					user = this.parent.metadata.authenticate();
				}
			}
		} catch (OpenemsNamedException e) {
			// login using session_id failed. Still keeping the WebSocket opened to give the
			// user the chance to authenticate manually.
			try {
				wsData.send(new AuthenticateWithSessionIdFailedNotification());
			} catch (OpenemsException e1) {
				this.parent.logWarn(this.log, e.getMessage());
			}
			return;
		}

		// store userId together with the WebSocket
		wsData.setUserId(user.getId());

		// generate token
		UUID token = UUID.randomUUID();
		wsData.setToken(token);

		// send connection successful reply
		AuthenticateWithSessionIdNotification notification = new AuthenticateWithSessionIdNotification(
				user.getSessionId(), user.getEdgeMetadatas(this.parent.metadata));
		this.parent.server.sendMessage(ws, notification);

		this.parent.logInfo(this.log, "User [" + user.getId() + ":" + user.getName() + "] connected.");
	}

}
