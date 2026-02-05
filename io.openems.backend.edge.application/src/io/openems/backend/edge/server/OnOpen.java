package io.openems.backend.edge.server;

import static io.openems.common.websocket.WebsocketUtils.getAsString;
import static io.openems.common.websocket.WebsocketUtils.parseRemoteIdentifier;
import static org.java_websocket.framing.CloseFrame.REFUSE;

import java.util.function.Function;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);

	private final Function<String, String> authenticateApikey;
	private final Runnable connectedEdgesChanged;

	public OnOpen(//
			Function<String, String> authenticateApikey, //
			Runnable connectedEdgesChanged) {
		this.authenticateApikey = authenticateApikey;
		this.connectedEdgesChanged = connectedEdgesChanged;
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshakedata handshakedata) {
		// get apikey from handshake
		final var apikey = getAsString(handshakedata, "apikey");

		var error = this._apply(ws, apikey);
		if (error != null) {
			ws.closeConnection(REFUSE, new StringBuilder() //
					.append("Connection to backend failed. Apikey [") //
					.append(apikey).append("]. Remote [") //
					.append(parseRemoteIdentifier(ws, handshakedata)) //
					.append("] Error: ").append(error.name()) //
					.toString());
		}
		return error;
	}

	private OpenemsError _apply(WebSocket ws, String apikey) {
		// get websocket attachment
		final WsData wsData = ws.getAttachment();

		if (apikey == null) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}

		// authenticate apikey
		var edgeId = this.authenticateApikey.apply(apikey);
		if (edgeId == null) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}

		// announce Edge as online
		wsData.setEdgeId(edgeId);
		wsData.debugLog(this.log, () -> "OPEN " + edgeId);

		this.connectedEdgesChanged.run();

		return null; // No error
	}
}
