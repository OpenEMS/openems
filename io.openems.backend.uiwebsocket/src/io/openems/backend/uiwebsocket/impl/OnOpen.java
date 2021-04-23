package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
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

		final AuthTuple authTuple;
		try {
			authTuple = this.authenticate(handshake);
		} catch (OpenemsNamedException e) {
			// login using token/session_id failed. Still keeping the WebSocket opened to
			// give the user the chance to authenticate manually.
			try {
				wsData.send(new AuthenticateWithSessionIdFailedNotification());
			} catch (OpenemsException e1) {
				this.parent.logWarn(this.log, e.getMessage());
			}
			return;
		}

		// store userId together with the WebSocket
		wsData.setUserId(authTuple.user.getId());

		// generate token
		wsData.setToken(authTuple.token);

		// send connection successful reply
		AuthenticateWithSessionIdNotification notification = new AuthenticateWithSessionIdNotification(authTuple.token,
				authTuple.user, User.generateEdgeMetadatas(authTuple.user, this.parent.metadata));
		this.parent.server.sendMessage(ws, notification);

		this.parent.logInfo(this.log,
				"User [" + authTuple.user.getId() + ":" + authTuple.user.getName() + "] connected.");
	}

	private static class AuthTuple {
		protected final User user;
		protected final String token;

		public AuthTuple(User user, String token) {
			this.user = user;
			this.token = token;
		}
	}

	private AuthTuple authenticate(JsonObject handshake) throws OpenemsNamedException {
		// authenticate with Token
		Optional<String> tokenOpt = io.openems.common.websocket.OnOpen.getFieldFromHandshakeCookie(handshake, "token");
		if (tokenOpt.isPresent()) {
			try {
				String token = tokenOpt.get();
				return new AuthTuple(this.parent.metadata.authenticate(token), token);

			} catch (OpenemsException e) {
				// ignore; try with Session-ID
			}
		}

		// authenticate with Session-ID
		Optional<String> sessionIdOpt = io.openems.common.websocket.OnOpen.getFieldFromHandshakeCookie(handshake,
				"session_id");
		if (!sessionIdOpt.isPresent()) {
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}

		String sessionId = sessionIdOpt.get();
		return new AuthTuple(this.parent.metadata.authenticate(sessionId), sessionId);
	}
}
