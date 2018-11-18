package io.openems.backend.uiwebsocket.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.notification.UiAuthenticateWithSessionId;
import io.openems.common.jsonrpc.notification.UiAuthenticateWithSessionId.EdgeMetadata;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public class OnOpen implements io.openems.common.websocket.OnOpen {

//	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final UiWebsocketImpl parent;

	public OnOpen(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) throws OpenemsException {
		User user;

		// login using session_id from the cookie
		Optional<String> sessionIdOpt = JsonUtils.getAsOptionalString(handshake, "session_id");
		try {
			if (sessionIdOpt.isPresent()) {
				// authenticate with Session-ID
				user = this.parent.metadata.authenticate(sessionIdOpt.get());
			} else {
				// authenticate without Session-ID
				user = this.parent.metadata.authenticate();
			}

		} catch (OpenemsException e) {
			// login using session_id failed. Still keeping the websocket opened to give the
			// user the change to authenticate manually.
			return;
		}

		// store userId together with the websocket
		WsData wsData = ws.getAttachment();
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
		UiAuthenticateWithSessionId notification = new UiAuthenticateWithSessionId(token, metadatas);
		this.parent.server.sendMessage(ws, notification);
	}

}
