package io.openems.backend.edge.manager;

import java.util.Optional;
import java.util.function.Function;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.backend.common.metadata.Edge;
import io.openems.common.logger.ContextLogger;

public class OnClose implements io.openems.common.websocket.OnClose {

	private final Logger log;
	private final Function<String, Optional<Edge>> getEdge;

	public OnClose(//
			String name, //
			Function<String, Optional<Edge>> getEdge //
	) {
		this.log = new ContextLogger(OnClose.class, name);
		this.getEdge = getEdge;
	}

	@Override
	public void accept(WebSocket ws, int code, String reason, boolean remote) {
		WsData wsData = ws.getAttachment();

		var edgeIds = wsData.onClose();
		for (var edgeId : edgeIds) {
			this.getEdge.apply(edgeId).ifPresent(edge -> {
				edge.setOnline(false);
			});
		}

		// TODO send notification, to UI

		this.log.info("Backend.Edge.Client [{}] disconnected. Reason: {}", wsData.getId(), reason);
	}

}
