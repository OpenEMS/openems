package io.openems.backend.edge.manager;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.Edge;

public class OnClose implements io.openems.common.websocket.OnClose {

	private final Logger log = LoggerFactory.getLogger(OnClose.class);
	private final Function<String, Optional<Edge>> getEdge;
	private final BiConsumer<Logger, String> logInfo;

	public OnClose(//
			Function<String, Optional<Edge>> getEdge, //
			BiConsumer<Logger, String> logInfo) {
		this.getEdge = getEdge;
		this.logInfo = logInfo;
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

		this.logInfo.accept(this.log, "Backend.Edge.Client [" + wsData.getId() + "] disconnected");
	}

}
