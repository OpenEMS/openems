package io.openems.backend.edge.client;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.backend.common.edge.jsonrpc.UpdateMetadataCache;
import io.openems.backend.edge.application.BackendEdgeServerApp;
import io.openems.backend.metrics.prometheus.PrometheusMetrics;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.AllSyncableEntriesNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final String name;
	private final Logger logger;
	private final BackendEdgeServerApp app;

	public OnNotification(//
			String name, //
			Logger logger, //
			BackendEdgeServerApp app) {
		this.name = name;
		this.logger = logger;
		this.app = app;
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) throws OpenemsNamedException {
		try (final var timer = PrometheusMetrics.WEBSOCKET_REQUEST
				.labelValues(this.name, notification.getFullyQualifiedMethod()).startTimer()) {
			switch (notification.getMethod()) {
			case UpdateMetadataCache.Notification.METHOD //
				-> this.app.getCache().update(UpdateMetadataCache.Notification.from(notification));
			case EdgeRpcNotification.METHOD //
				-> this.handleEdgeRpcNotification(EdgeRpcNotification.from(notification));
			case AllSyncableEntriesNotification.METHOD ->
				this.handleSyncableEntriesUpdate(AllSyncableEntriesNotification.from(notification));
			default //
				-> this.logger.warn("Unhandled Notification from Backend: {}", notification);
			}
		}
	}

	private void handleEdgeRpcNotification(EdgeRpcNotification n) {
		this.app.sendNotificationToEdge(n.getEdgeId(), n.getPayload());
	}

	private void handleSyncableEntriesUpdate(AllSyncableEntriesNotification n) {
		this.logger.info("Received syncable entry update.");
		this.app.getSyncableRepository().replaceStaticEntries(n.getEntries());
	}

}
