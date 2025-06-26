package io.openems.backend.edgewebsocket;

import static io.openems.common.websocket.WebsocketUtils.getAsString;
import static io.openems.common.websocket.WebsocketUtils.parseRemoteIdentifier;
import static org.java_websocket.framing.CloseFrame.REFUSE;
import static org.java_websocket.framing.CloseFrame.TRY_AGAIN_LATER;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;

import io.openems.common.exceptions.OpenemsError;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final EdgeWebsocketImpl parent;

	public OnOpen(EdgeWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshakedata handshakedata) {
		// get apikey from handshake
		final var apikey = getAsString(handshakedata, "apikey");

		var error = this._apply(ws, apikey);
		if (error != null) {
			if (this.parent.metadata.isInitialized()) {
				ws.closeConnection(REFUSE, "Connection to backend failed. " //
						+ "Apikey [" + apikey + "]. " //
						+ "Remote [" + parseRemoteIdentifier(ws, handshakedata) + "] " //
						+ "Error: " + error.name());
			} else {
				ws.closeConnection(TRY_AGAIN_LATER, "Connection to backend failed. Metadata is not yet initialized. " //
						+ "Apikey [" + apikey + "]. " //
						+ "Remote [" + parseRemoteIdentifier(ws, handshakedata) + "] " //
						+ "Error: " + error.name());
			}
		}
		return error;
	}

	private OpenemsError _apply(WebSocket ws, String apikey) {
		// get websocket attachment
		final WsData wsData = ws.getAttachment();

		if (apikey == null) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}

		// get edgeId for apikey
		var edgeIdOpt = this.parent.metadata.getEdgeIdForApikey(apikey);
		if (!edgeIdOpt.isPresent()) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}
		var edgeId = edgeIdOpt.get();

		// get metadata for Edge
		var edgeOpt = this.parent.metadata.getEdge(edgeId);
		if (!edgeOpt.isPresent()) {
			return OpenemsError.COMMON_SERVICE_NOT_AVAILABLE;
		}
		var edge = edgeOpt.get();

		// announce Edge as online
		edge.setOnline(true);
		edge.setLastmessage();
		wsData.setEdgeId(edgeId);

		return null; // No error
	}
}
