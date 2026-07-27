package io.openems.backend.edge.server;

import static io.openems.common.websocket.WebsocketUtils.parseRemoteIdentifier;
import static org.java_websocket.framing.CloseFrame.REFUSE;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final Runnable connectedEdgesChanged;

	public OnOpen(Runnable connectedEdgesChanged) {
		this.connectedEdgesChanged = connectedEdgesChanged;
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshakedata handshakedata) {
		var error = this._apply(ws);
		if (error != null) {
			ws.closeConnection(REFUSE, "Connection to backend failed. Remote [" //
					+ parseRemoteIdentifier(ws, handshakedata) //
					+ "] Error: " + error.name() //
			);
		}
		return error;
	}

	private OpenemsError _apply(WebSocket ws) {
		// get websocket attachment
		WsData wsData = ws.getAttachment();

		// authenticate apikey
		var edgeId = wsData.getEdgeId();
		if (edgeId == null) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}

		wsData.debugLog(this.log, () -> "OPEN " + edgeId);

		this.connectedEdgesChanged.run();

		return null; // No error
	}
}
