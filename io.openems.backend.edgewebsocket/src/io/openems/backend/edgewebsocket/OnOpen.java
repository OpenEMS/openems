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
				throw new OpenemsException("Apikey is missing in handshake. " //
						+ "Remote [" + ws.getRemoteSocketAddress() + "]");
			}
			apikey = apikeyOpt.get().trim();

			// get edgeId for apikey
			var edgeIdOpt = this.parent.metadata.getEdgeIdForApikey(apikey);
			if (!edgeIdOpt.isPresent()) {
				throw new OpenemsException("Unable to authenticate this Apikey. " //
						+ "Remote [" + ws.getRemoteSocketAddress() + "]");
			}
			var edgeId = edgeIdOpt.get();

			// get metadata for Edge
			var edgeOpt = this.parent.metadata.getEdge(edgeId);
			if (!edgeOpt.isPresent()) {
				throw new OpenemsException("Unable to get metadata for Edge [" + edgeId + "]. " //
						+ "Remote [" + ws.getRemoteSocketAddress() + "]");
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
						+ "Remote [" + ws.getRemoteSocketAddress() + "] " //
						+ "Error: " + e.getMessage());
			} else {
				// close websocket
				ws.closeConnection(CloseFrame.TRY_AGAIN_LATER,
						"Connection to backend failed. Metadata is not yet initialized. " //
								+ "Apikey [" + apikey + "]. " //
								+ "Remote [" + ws.getRemoteSocketAddress() + "] " //
								+ "Error: " + e.getMessage());
			}
		}
	}

}
