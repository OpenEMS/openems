package io.openems.backend.edge.manager;

import static io.openems.common.websocket.WebsocketUtils.getAsString;
import static io.openems.common.websocket.WebsocketUtils.parseRemoteIdentifier;
import static org.java_websocket.framing.CloseFrame.REFUSE;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.edge.jsonrpc.UpdateMetadataCache;
import io.openems.common.exceptions.OpenemsError;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final Supplier<UpdateMetadataCache.Notification> generateUpdateMetadataCacheNotification;
	private final BiConsumer<Logger, String> logInfo;

	public OnOpen(//
			Supplier<UpdateMetadataCache.Notification> generateUpdateMetadataCacheNotification, //
			BiConsumer<Logger, String> logInfo) {
		this.generateUpdateMetadataCacheNotification = generateUpdateMetadataCacheNotification;
		this.logInfo = logInfo;
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshakedata handshakedata) {
		// get id from handshake
		final var id = getAsString(handshakedata, "id");

		var error = this._apply(ws, id);
		if (error != null) {
			// close websocket
			ws.closeConnection(REFUSE, "Connection to backend failed. " //
					+ "Remote [" + parseRemoteIdentifier(ws, handshakedata) + "] " //
					+ "Error: " + error.name());
		}
		return error;
	}

	private OpenemsError _apply(WebSocket ws, String id) {
		// get websocket attachment
		final WsData wsData = ws.getAttachment();

		if (id == null) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}

		this.logInfo.accept(this.log, "Backend.Edge.Client [" + id + "] connected");

		wsData.setId(id);

		// Send a UpdateMetadataCache.Notification
		wsData.send(this.generateUpdateMetadataCacheNotification.get());

		return null; // No error
	}
}
