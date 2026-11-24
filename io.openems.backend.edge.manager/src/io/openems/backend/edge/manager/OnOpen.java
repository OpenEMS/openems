package io.openems.backend.edge.manager;

import static io.openems.common.utils.StringUtils.isNullOrBlank;
import static io.openems.common.websocket.CloseCode.REFUSE;
import static io.openems.common.websocket.WebsocketUtils.getAsStringOrNull;
import static io.openems.common.websocket.WebsocketUtils.parseRemoteIdentifier;

import org.slf4j.Logger;

import io.openems.common.bridge.http.api.HttpAuthorization;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.notification.AllSyncableEntriesNotification;
import io.openems.common.logger.ContextLogger;
import io.openems.common.websocket.Handshake;
import io.openems.common.websocket.WebSocket;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log;
	private final EdgeManagerImpl parent;

	public OnOpen(String name, EdgeManagerImpl parent) {
		this.log = new ContextLogger(OnOpen.class, name);
		this.parent = parent;
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshake handshake) {
		// get authorization from handshake
		final var authorization = getAsStringOrNull(handshake, HttpHeader.HEADER_AUTHORIZATION);

		var error = this._apply(ws, authorization);
		if (error != null) {
			// close websocket
			ws.close(REFUSE, "Connection to backend failed. " //
					+ "Remote [" + parseRemoteIdentifier(ws, handshake) + "] " //
					+ "Error: " + error.name());
		}
		return error;
	}

	private OpenemsError _apply(WebSocket ws, String authorizationHeader) {
		// get websocket attachment
		final WsData wsData = ws.getAttachment();

		final var authToken = HttpAuthorization.extractBearerToken(authorizationHeader);
		if (isNullOrBlank(authToken)) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}
		final var subject = this.parent.tokenVerifier.verify(authToken);
		if (isNullOrBlank(subject)) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}

		this.log.info("Backend.Edge.Client [{}] connected", subject);

		wsData.setId(subject);

		// Send a UpdateMetadataCache.Notification
		wsData.send(this.parent.metadata.generateUpdateMetadataCacheNotification());
		wsData.send(new AllSyncableEntriesNotification(this.parent.syncableRepository.getAllStaticEntries()));

		return null; // No error
	}
}
