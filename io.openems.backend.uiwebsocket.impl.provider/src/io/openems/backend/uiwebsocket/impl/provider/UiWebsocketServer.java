package io.openems.backend.uiwebsocket.impl.provider;

import java.util.Map.Entry;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Role;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.websocket.DefaultMessages;

public class UiWebsocketServer extends AbstractWebsocketServer {

	private final Logger log = LoggerFactory.getLogger(UiWebsocketServer.class);
	private final UiWebsocket parent;

	public UiWebsocketServer(UiWebsocket parent, int port) {
		super(port);
		this.parent = parent;
	}

	@Override
	protected void _onOpen(WebSocket websocket, ClientHandshake handshake) {
		log.info("OnOpen");
		String error = "";
		User user = null;

		// login using session_id from the cookie
		Optional<String> sessionIdOpt = getSessionIdFromHandshake(handshake);
		if (!sessionIdOpt.isPresent()) {
			error = "Session-ID is missing in handshake";
		} else {
			try {
				user = this.parent.getMetadataService().getUserWithSession(sessionIdOpt.get());
			} catch (OpenemsException e) {
				error = e.getMessage();
			}
		}

		if (!error.isEmpty()) {
			// send connection failed to browser
			this.send(websocket, DefaultMessages.browserConnectionFailedReply());
			log.warn("User connection failed. Session [" + sessionIdOpt.orElse("") + "] Error [" + error + "].");
			websocket.closeConnection(CloseFrame.REFUSE, error);
		} else if (user != null) {
			// send connection successful to browser
			for (Entry<Integer, Role> deviceRole : user.getDeviceRoles().entrySet()) {
				// boolean isOnline =
				// this.parent.getEdgeWebsocketService().isOnline(deviceRole.getKey());
				// JsonArray jDevices
			}

			//
			//
			// JsonObject jReply =
			// DefaultMessages.browserConnectionSuccessfulReply(session.getToken(),
			// Optional.empty(),
			// data.getDevices());
			// // TODO write user name to log output
			// WebSocketUtils.send(websocket, jReply);
			//
			// // add websocket to local cache
			// this.addWebsocket(websocket, session);
			// log.info("User [" + data.getUserName() + "] connected with Session [" +
			// data.getOdooSessionId().orElse("")
			// + "].");

		}

		// // check if the session is now valid and send reply to browser
		// BrowserSessionData data = session.getData();
		// if (error.isEmpty()) {
		// // add isOnline information
		//

		//

	}

	@Override
	protected void _onMessage(WebSocket websocket, JsonObject jMessage, Optional<JsonArray> jMessageIdOpt,
			Optional<String> deviceNameOpt) {
		log.info("UiWebsocketServer: On Message");
	}

	@Override
	protected void _onError(WebSocket websocket, Exception ex) {
		log.info("UiWebsocketServer: On Error");
	}

	@Override
	protected void _onClose(WebSocket websocket) {
		log.info("UiWebsocketServer: On Close");
	}
}
