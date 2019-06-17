package io.openems.backend.uiwebsocket.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import io.openems.common.access_control.RoleId;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdFailedNotification;
import io.openems.common.jsonrpc.notification.AuthenticateWithSessionIdNotification;
import io.openems.common.jsonrpc.shared.EdgeMetadata;
import io.openems.common.session.Role;

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

		RoleId roleId = this.parent.accessControl.login("Kartoffelsalat3000", "user");
		wsData.setRoleId(roleId);
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
		List<EdgeMetadata> metadatas = new ArrayList<>();
		for (Entry<String, Role> edgeRole : user.getEdgeRoles().entrySet()) {
			String edgeId = edgeRole.getKey();
            Optional<Edge> edgeOpt = this.parent.metadata.getEdge(edgeId);
			if (edgeOpt.isPresent()) {
				Edge e = edgeOpt.get();
				metadatas.add(new EdgeMetadata(//
						e.getId(), // Edge-ID
						e.getComment(), // Comment
						e.getProducttype(), // Product-Type
						e.getVersion(), // Version
						roleId, // Role
						e.isOnline() // Online-State
				));
			}
		}

		AuthenticateWithSessionIdNotification notification = new AuthenticateWithSessionIdNotification(token,
				metadatas);
		this.parent.server.sendMessage(ws, notification);

		this.parent.logInfo(this.log, "User [" + user.getId() + ":" + user.getName() + "] connected.");
	}

}
