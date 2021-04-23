package io.openems.edge.controller.api.websocket;

import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdFailedNotification;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdNotification;
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

		final AuthTuple authTuple;
		try {
			authTuple = this.authenticate(handshake);
		} catch (OpenemsNamedException e) {
			// login using token/session_id failed. Still keeping the WebSocket opened to
			// give the user the chance to authenticate manually.

			// generate new, random Session Token
			wsData.setSessionToken(UUID.randomUUID().toString());
			try {
				wsData.send(new AuthenticateWithSessionIdFailedNotification());
			} catch (OpenemsException e1) {
				this.parent.logWarn(this.log, e.getMessage());
			}
			return;
		}

		// store token in attachment
		wsData.setSessionToken(authTuple.token);

		// store user in attachment
		wsData.setUser(authTuple.user);

		// send connection successful reply
		AuthenticateWithSessionIdNotification notification = new AuthenticateWithSessionIdNotification(authTuple.token,
				authTuple.user, Utils.getEdgeMetadata(authTuple.user.getRole()));
		this.parent.server.sendMessage(ws, notification);

		// log
		this.parent.logInfo(this.log, "User [" + authTuple.user.getName() + "] logged in by token");
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
		if (!tokenOpt.isPresent()) {
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}
		String token = tokenOpt.get();
		User user = this.parent.sessionTokens.get(token);
		if (user == null) {
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}
		return new AuthTuple(this.parent.sessionTokens.get(token), token);
	}
}
