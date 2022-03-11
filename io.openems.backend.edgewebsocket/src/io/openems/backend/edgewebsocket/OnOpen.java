package io.openems.backend.edgewebsocket;

import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
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
			apikey = apikeyOpt.get();
			wsData.setApikey(apikey);

			// get edgeId for apikey
			var edgeIdOpt = this.parent.metadata.getEdgeIdForApikey(apikey);
			if (!edgeIdOpt.isPresent()) {
				throw new OpenemsException("Unable to authenticate this Apikey.");
			}
			var edgeId = edgeIdOpt.get();
			wsData.setEdgeId(edgeId);

			// get metadata for Edge
			var edgeOpt = this.parent.metadata.getEdge(edgeId);
			if (!edgeOpt.isPresent()) {
				throw new OpenemsException("Unable to get metadata for Edge [" + edgeId + "]");
			}
			var edge = edgeOpt.get();

			// log
			this.parent.logInfo(this.log, "Edge [" + edge.getId() + "] connected.");

			// announce Edge as online
			edge.setOnline(true);
			edge.setLastMessageTimestamp();
			wsData.setAuthenticated(true);

			// TODO send notification to UI
		} catch (OpenemsException e) {
			if (this.parent.metadata.isInitialized()) {
				// close websocket
				ws.closeConnection(CloseFrame.REFUSE,
						"Connection to backend failed. Apikey [" + apikey + "]. Error: " + e.getMessage());
			} else {
				// close websocket
				ws.closeConnection(CloseFrame.TRY_AGAIN_LATER,
						"Connection to backend failed. Metadata is not yet initialized. Apikey [" + apikey
								+ "]. Error: " + e.getMessage());
			}
		}
	}

}
