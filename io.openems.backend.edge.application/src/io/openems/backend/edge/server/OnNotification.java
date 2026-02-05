package io.openems.backend.edge.server;

import static io.openems.common.utils.JsonrpcUtils.simplifyJsonrpcMessage;
import static io.openems.common.utils.StringUtils.toShortString;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.function.BiConsumer;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metrics.prometheus.PrometheusMetrics;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final Logger log = LoggerFactory.getLogger(OnNotification.class);

	private final String name;
	private final BiConsumer<String, JsonrpcNotification> sendToEdgeManager;

	public OnNotification(//
			String name, //
			BiConsumer<String, JsonrpcNotification> sendToEdgeManager) {
		this.name = name;
		this.sendToEdgeManager = sendToEdgeManager;
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) {
		try (final var timer = PrometheusMetrics.WEBSOCKET_REQUEST
				.labelValues(this.name, notification.getFullyQualifiedMethod()).startTimer()) {
			WsData wsData = ws.getAttachment();
			var edgeId = wsData.getEdgeIdWithTimeout(10, SECONDS);
			if (edgeId == null) {
				return;
			}
			wsData.debugLog(this.log,
					() -> "NOTIFICATION " + edgeId + ": " + toShortString(simplifyJsonrpcMessage(notification), 200));
			this.sendToEdgeManager.accept(edgeId, notification);
		}
	}
}
