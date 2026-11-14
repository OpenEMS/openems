package io.openems.backend.edge.server;

import static io.openems.common.utils.JsonrpcUtils.simplifyJsonrpcMessage;
import static io.openems.common.utils.StringUtils.toShortString;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.edge.application.BackendEdgeServerApp;
import io.openems.backend.metrics.prometheus.PrometheusMetrics;
import io.openems.common.jsonrpc.base.JsonrpcNotification;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final Logger log = LoggerFactory.getLogger(OnNotification.class);

	private final String name;
	private final BackendEdgeServerApp app;

	public OnNotification(//
			String name, //
			BackendEdgeServerApp app) {
		this.name = name;
		this.app = app;
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) {
		try (final var timer = PrometheusMetrics.WEBSOCKET_REQUEST
				.labelValues(this.name, notification.getFullyQualifiedMethod()).startTimer()) {
			WsData wsData = ws.getAttachment();
			var edgeId = wsData.getEdgeId();
			if (edgeId == null) {
				return;
			}
			wsData.debugLog(this.log,
					() -> "NOTIFICATION " + edgeId + ": " + toShortString(simplifyJsonrpcMessage(notification), 200));
			this.app.sendNotificationToEdgeManager(edgeId, notification);
		}
	}
}
