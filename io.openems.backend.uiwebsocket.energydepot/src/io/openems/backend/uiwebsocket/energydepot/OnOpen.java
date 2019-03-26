package io.openems.backend.uiwebsocket.energydepot;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.BackendUser;
import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdFailedNotification;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdNotification;
import io.openems.common.jsonrpc.shared.EdgeMetadata;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final UiWebsocketKaco parent;

	public OnOpen(UiWebsocketKaco parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) throws OpenemsException {
		// get websocket attachment
		WsData wsData = ws.getAttachment();
		this.parent.logInfo(this.log, "Handshake: " + handshake.toString());
		// declare user
		BackendUser user;
		String authorization;

		try {
			authorization = JsonUtils.getAsString(handshake, "Authorization");
		} catch (OpenemsNamedException e1) {
			// TODO Auto-generated catch block
			authorization = null;
		}
		try {
			if (authorization == null || !authorization.toLowerCase().startsWith("basic")) {

				// login using session_id from the handshake
				Optional<String> sessionIdOpt = io.openems.common.websocket.OnOpen
						.getFieldFromHandshakeCookie(handshake, "wordpress_logged_in_c92c39b1a6e355483164923c7de6f7b7");
				try {
					if (sessionIdOpt.isPresent()) {
						// authenticate with Session-ID
						user = this.parent.metadata.authenticate(sessionIdOpt.get());
					} else {
						// authenticate without Session-ID
						user = this.parent.metadata.authenticate();
					}
				} catch (OpenemsNamedException e) {
					// login using session_id failed. Still keeping the WebSocket opened to give the
					// user the chance to authenticate manually.
					wsData.send(new AuthenticateWithSessionIdFailedNotification());
					return;
				}

			} else {

				String base64Credentials = authorization.substring("Basic".length()).trim();
				byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
				String credentials = new String(credDecoded, StandardCharsets.UTF_8);
				// credentials = username:password
				final String[] values = credentials.split(":", 2);
				if (values.length != 2) {
					throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
				}
				String username = values[0];
				String password = values[1];

				user = this.parent.metadata.authenticate(username, password);

			}
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			this.parent.logWarn(this.log, e.toString());
			e.printStackTrace();
			return;
		}
		// store userId together with the WebSocket
		wsData.setUserId(user.getId());

		// generate token
		UUID token = UUID.randomUUID();
		wsData.setToken(token);

		// send connection successful reply
		List<EdgeMetadata> metadatas = new ArrayList<>();
		for (Entry<String, Role> edgeRole : user.getEdgeRoles().entrySet()) {
			String edgeId = edgeRole.getKey();
			Role role = edgeRole.getValue();
			Optional<Edge> edgeOpt = this.parent.metadata.getEdge(edgeId);
			if (edgeOpt.isPresent()) {
				Edge e = edgeOpt.get();
				metadatas.add(new EdgeMetadata(e.getId(), e.getComment(), e.getProducttype(), e.getVersion(), role,
						e.isOnline()));
			}
		}

		AuthenticateWithSessionIdNotification notification = new AuthenticateWithSessionIdNotification(token,
				metadatas);
		this.parent.server.sendMessage(ws, notification);

		this.parent.logInfo(this.log, "User [" + user.getId() + ":" + user.getName() + "] connected.");
	}

}
