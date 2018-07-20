package io.openems.backend.uiwebsocket.impl.provider;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.websocket.AbstractOnOpen;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.WebSocketUtils;

public class OnOpen extends AbstractOnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final UiWebsocketServer parent;

	public OnOpen(UiWebsocketServer parent, WebSocket websocket, ClientHandshake handshake) {
		super(websocket, handshake);
		this.parent = parent;
	}

	@Override
	protected void run(WebSocket websocket, ClientHandshake handshake) {
		// create websocket attachment
		WebsocketData attachment = new WebsocketData();
		websocket.setAttachment(attachment);

		User user;

		// login using session_id from the cookie
		Optional<String> sessionIdOpt = AbstractOnOpen.getFieldFromHandshakeCookie(handshake, "wordpress_logged_in_c92c39b1a6e355483164923c7de6f7b7");
		try {
			if (sessionIdOpt.isPresent()) {
				// authenticate with Session-ID
				user = this.parent.parent.metadataService.authenticate(sessionIdOpt.get());
			} else {
				// authenticate without Session-ID
				user = this.parent.parent.metadataService.authenticate();
			}
		} catch (OpenemsException e) {
			// send connection failed to browser
			WebSocketUtils.sendOrLogError(websocket, DefaultMessages.uiLogoutReply());
			log.warn("User connection failed. Session [" + sessionIdOpt.orElse("") + "] Error [" + e.getMessage()
					+ "].");
			websocket.closeConnection(CloseFrame.REFUSE, e.getMessage());
			return;
		}

		UUID uuid = UUID.randomUUID();
		synchronized (this.parent.websocketsMap) {
			// add websocket to local cache
			this.parent.websocketsMap.put(uuid, websocket);
		}
		// store userId together with the websocket
		attachment.initialize(user.getId(), uuid);

		// send connection successful to browser
		JsonArray jEdges = new JsonArray();
		for (Entry<Integer, Role> edgeRole : user.getEdgeRoles().entrySet()) {
			int edgeId = edgeRole.getKey();
			Role role = edgeRole.getValue();
			Edge edge;
			try {
				edge = this.parent.parent.metadataService.getEdge(edgeId);
				JsonObject jEdge = edge.toJsonObject();
				jEdge.addProperty("role", role.toString());
				jEdges.add(jEdge);
			} catch (OpenemsException e) {
				log.warn("Unable to get Edge from MetadataService [ID:" + edgeId + "]: " + e.getMessage());
			}
		}
		log.info("User [" + user.getName() + "] connected with Session [" + sessionIdOpt.orElse("") + "].");
		JsonObject jReply = DefaultMessages.uiLoginSuccessfulReply("" /* empty token? */, jEdges);
		WebSocketUtils.sendOrLogError(websocket, jReply);
	}

}
