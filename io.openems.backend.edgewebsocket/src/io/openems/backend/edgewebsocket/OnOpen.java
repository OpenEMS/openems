package io.openems.backend.edgewebsocket;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final EdgeWebsocketImpl parent;

	public OnOpen(EdgeWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonObject handshake) {
		// get websocket attachment
		WsData wsData = ws.getAttachment();

		var apikey = "";
		try {
			// get apikey from handshake
			var apikeyOpt = JsonUtils.getAsOptionalString(handshake, "apikey");
			if (!apikeyOpt.isPresent()) {
				throw new OpenemsException("Apikey is missing in handshake");
			}
			apikey = apikeyOpt.get().trim();

			// get edgeId for apikey
			var edgeIdOpt = this.parent.metadata.getEdgeIdForApikey(apikey);
			if (!edgeIdOpt.isPresent()) {
				throw new OpenemsException("Unable to authenticate this Apikey");
			}
			var edgeId = edgeIdOpt.get();

			// get metadata for Edge
			var edgeOpt = this.parent.metadata.getEdge(edgeId);
			if (!edgeOpt.isPresent()) {
				throw new OpenemsException("Unable to get metadata for Edge [" + edgeId + "]");
			}
			var edge = edgeOpt.get();

			// announce Edge as online
			edge.setOnline(true);
			edge.setLastmessage();
			wsData.setEdgeId(edgeId);

			// TODO send notification to UI
		} catch (OpenemsException e) {
			if (this.parent.metadata.isInitialized()) {
				// close websocket
				ws.closeConnection(CloseFrame.REFUSE, "Connection to backend failed. " //
						+ "Apikey [" + apikey + "]. " //
						+ "Remote [" + parseRemoteIdentifier(ws, handshake) + "] " //
						+ "Error: " + e.getMessage());
			} else {
				// close websocket
				ws.closeConnection(CloseFrame.TRY_AGAIN_LATER,
						"Connection to backend failed. Metadata is not yet initialized. " //
								+ "Apikey [" + apikey + "]. " //
								+ "Remote [" + parseRemoteIdentifier(ws, handshake) + "] " //
								+ "Error: " + e.getMessage());
			}
		}
	}

	/**
	 * Parses a identifier for the Remote from the handshake.
	 * 
	 * <p>
	 * Tries to use the headers "Forwarded", "X-Forwarded-For" or "X-Real-IP". Falls
	 * back to `ws.getRemoteSocketAddress()`. See https://serverfault.com/a/920060
	 * 
	 * @param ws        the {@link WebSocket}
	 * @param handshake the Handshake
	 * @return an identifier String
	 */
	private static String parseRemoteIdentifier(WebSocket ws, JsonObject handshake) {
		for (var key : REMOTE_IDENTIFICATION_HEADERS) {
			var value = JsonUtils.getAsOptionalString(handshake,
					key.toLowerCase() /* handshake keys are all lower case */);
			if (value.isPresent()) {
				return value.get();
			}
		}
		// fallback
		return ws.getRemoteSocketAddress().toString();
	}

	private static final String[] REMOTE_IDENTIFICATION_HEADERS = new String[] { //
			"Forwarded", "X-Forwarded-For", "X-Real-IP" };

}
