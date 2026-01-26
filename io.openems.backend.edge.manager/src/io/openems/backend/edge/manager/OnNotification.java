package io.openems.backend.edge.manager;

import static io.openems.common.channel.Level.FAULT;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsPrimitive;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.java_websocket.WebSocket;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.edge.jsonrpc.ConnectedEdges;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Edge.Events;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.backend.common.uiwebsocket.UiWebsocket;
import io.openems.backend.metrics.prometheus.PrometheusMetrics;
import io.openems.common.channel.Level;
import io.openems.common.event.EventBuilder;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.AbstractDataNotification;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.LogMessageNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.SemanticVersion;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final Logger log = LoggerFactory.getLogger(OnNotification.class);

	private final String name;
	private final Supplier<EventAdmin> eventAdmin;
	private final Supplier<UiWebsocket> uiWebsocket;
	private final Supplier<TimedataManager> timedataManager;
	private final Function<String, Optional<Edge>> getEdge;
	private final BiConsumer<String, SystemLogNotification> handleSystemLogNotification;
	private final BiConsumer<Logger, String> logInfo;
	private final BiConsumer<Logger, String> logWarn;

	public OnNotification(//
			String name, //
			Supplier<EventAdmin> eventAdmin, //
			Supplier<UiWebsocket> uiWebsocket, //
			Supplier<TimedataManager> timedataManager, //
			Function<String, Optional<Edge>> getEdge, //
			BiConsumer<String, SystemLogNotification> handleSystemLogNotification, //
			BiConsumer<Logger, String> logInfo, //
			BiConsumer<Logger, String> logWarn) {
		this.name = name;
		this.eventAdmin = eventAdmin;
		this.uiWebsocket = uiWebsocket;
		this.timedataManager = timedataManager;
		this.getEdge = getEdge;
		this.handleSystemLogNotification = handleSystemLogNotification;
		this.logInfo = logInfo;
		this.logWarn = logWarn;
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) throws OpenemsNamedException {
		try (final var timer = PrometheusMetrics.WEBSOCKET_REQUEST
				.labelValues(this.name, notification.getFullyQualifiedMethod()).startTimer()) {
			WsData wsData = ws.getAttachment();

			switch (notification.getMethod()) {
			case ConnectedEdges.Notification.METHOD //
				-> this.handleConnectedEdgesNotification(ConnectedEdges.Notification.from(notification), wsData);
			case EdgeRpcNotification.METHOD //
				-> this.handleEdgeRpcNotification(EdgeRpcNotification.from(notification), wsData);
			default //
				-> this.logWarn.accept(this.log, "Unhandled Notification: " + notification);
			}
		}
	}

	/**
	 * Handles a {@link ConnectedEdges.Notification}.
	 *
	 * @param notification the {@link ConnectedEdges.Notification}
	 * @param wsData       the {@link WsData}
	 */
	private void handleConnectedEdgesNotification(ConnectedEdges.Notification notification, WsData wsData) {
		wsData.handleConnectedEdgesNotification(notification, //
				this::announceOnline, //
				this::announceOffline, //
				metrics -> {

					// TODO should be moved to generic metric component
					metrics.getData().rowMap().entrySet().stream().flatMap(t -> t.getValue().entrySet().stream())
							.forEach(t -> {
								try {
									final var channelAddress = ChannelAddress.fromString(t.getKey());
									final var metricCollector = switch (channelAddress.getChannelId()) {
									case "Connections" -> PrometheusMetrics.WEBSOCKET_CONNECTION;
									case "Pending" -> PrometheusMetrics.THREAD_POOL_QUEUE;
									case "Active" -> PrometheusMetrics.THREAD_POOL_ACTIVE_COUNT;
									case "Completed" -> PrometheusMetrics.THREAD_POOL_COMPLETED_TASKS;
									case "PoolSize" -> PrometheusMetrics.THREAD_POOL_CURRENT_SIZE;
									case "MaxPoolSize" -> PrometheusMetrics.THREAD_POOL_MAX_SIZE;
									default -> null;
									};
									if (metricCollector == null) {
										return;
									}
									metricCollector.labelValues(channelAddress.getComponentId())
											.set(t.getValue().getAsDouble());
								} catch (OpenemsNamedException e) {
									this.log.error(e.getMessage(), e);
								}
							});

					final var timedataManager = this.timedataManager.get();
					if (timedataManager != null) {
						timedataManager.write("backend0", metrics);
					}
				});
	}

	/**
	 * Handles a {@link EdgeRpcNotification}.
	 *
	 * @param n      the {@link EdgeConfigNotification}
	 * @param wsData the {@link WsData}
	 */
	private void handleEdgeRpcNotification(EdgeRpcNotification n, WsData wsData) throws OpenemsNamedException {
		final var edgeId = n.getEdgeId();
		var notification = n.getPayload();

		// announce incoming message for this Edge
		this.getEdge.apply(edgeId).ifPresent(edge -> {
			announceOnline(edge);
		});

		// Handle notification
		switch (notification.getMethod()) {
		case EdgeConfigNotification.METHOD //
			-> this.handleEdgeConfigNotification(EdgeConfigNotification.from(notification), edgeId);
		case TimestampedDataNotification.METHOD //
			-> this.handleDataNotification(TimestampedDataNotification.from(notification), edgeId, wsData);
		case AggregatedDataNotification.METHOD //
			-> this.handleDataNotification(AggregatedDataNotification.from(notification), edgeId, wsData);
		case ResendDataNotification.METHOD //
			-> this.handleResendDataNotification(ResendDataNotification.from(notification), edgeId);
		case SystemLogNotification.METHOD //
			-> this.handleSystemLogNotification(SystemLogNotification.from(notification), edgeId);
		case LogMessageNotification.METHOD //
			-> this.handleLogMessageNotification(LogMessageNotification.from(notification), edgeId);
		default //
			-> this.logWarn.accept(this.log, "[" + edgeId + "] Unhandled Notification: " + notification);
		}
	}

	/**
	 * Handles a {@link EdgeConfigNotification}.
	 *
	 * @param message the {@link EdgeConfigNotification}
	 * @param edgeId  the Edge-ID
	 */
	private void handleEdgeConfigNotification(EdgeConfigNotification message, String edgeId) {
		// Save config in metadata
		var edgeOpt = this.getEdge.apply(edgeId);
		if (edgeOpt.isPresent()) {
			var edge = edgeOpt.get();
			EventBuilder.from(this.eventAdmin.get(), Events.ON_SET_CONFIG) //
					.addArg(Events.OnSetConfig.EDGE, edge) //
					.addArg(Events.OnSetConfig.CONFIG, message.getConfig()) //
					.send(); //
		}

		// Forward to UI sessions
		var uiWebsocket = this.uiWebsocket.get();
		if (uiWebsocket != null) {
			uiWebsocket.sendBroadcast(edgeId, new EdgeRpcNotification(edgeId, message));
		}
	}

	/**
	 * Handles a {@link AbstractDataNotification}.
	 *
	 * @param message the {@link AbstractDataNotification}
	 * @param edgeId  the Edge-ID
	 * @param wsData  the {@link WsData}
	 */
	private void handleDataNotification(AbstractDataNotification message, String edgeId, WsData wsData)
			throws OpenemsNamedException {
		final var edgeCache = wsData.getEdgeCache(edgeId);
		if (edgeCache == null) {
			return;
		}

		final var timedataManager = this.timedataManager.get();
		if (timedataManager != null) {
			// TODO java 21 switch case with type
			if (message instanceof TimestampedDataNotification timestampNotification) {
				edgeCache.updateCurrentData(timestampNotification);
				timedataManager.write(edgeId, timestampNotification);
			} else if (message instanceof AggregatedDataNotification aggregatedNotification) {
				edgeCache.updateAggregatedData(aggregatedNotification);
				timedataManager.write(edgeId, aggregatedNotification);
			}
		}

		// Forward subscribed Channels to UI
		final var uiWebsocket = this.uiWebsocket.get();
		if (uiWebsocket != null) {
			uiWebsocket.sendSubscribedChannels(edgeId, edgeCache);
		}

		// Read some specific channels
		var edgeOpt = this.getEdge.apply(edgeId);
		if (edgeOpt.isPresent()) {
			var edge = edgeOpt.get();
			for (var entry : message.getParams().entrySet()) {
				var d = getAsJsonObject(entry.getValue());

				// set specific Edge values
				if (d.has("_sum/State") && d.get("_sum/State").isJsonPrimitive()) {
					var sumState = Level.fromJson(d, "_sum/State").orElse(FAULT);
					edge.setSumState(sumState);
				}

				if (d.has("_meta/Version") && d.get("_meta/Version").isJsonPrimitive()) {
					var version = getAsPrimitive(d, "_meta/Version").getAsString();
					edge.setVersion(SemanticVersion.fromString(version));
				}
			}
		}
	}

	/**
	 * Handles a {@link ResendDataNotification}.
	 *
	 * @param message the {@link ResendDataNotification}
	 * @param edgeId  the Edge-ID
	 */
	private void handleResendDataNotification(ResendDataNotification message, String edgeId)
			throws OpenemsNamedException {
		var timedataManager = this.timedataManager.get();
		if (timedataManager != null) {
			timedataManager.write(edgeId, message);
		}
	}

	/**
	 * Handles a {@link SystemLogNotification}.
	 *
	 * @param message the {@link SystemLogNotification}
	 * @param edgeId  the Edge-ID
	 */
	private void handleSystemLogNotification(SystemLogNotification message, String edgeId)
			throws OpenemsNamedException {
		this.handleSystemLogNotification.accept(edgeId, message);
	}

	/**
	 * Handles a {@link LogMessageNotification}. Logs given message from request.
	 *
	 * @param message the {@link LogMessageNotification}
	 * @param edgeId  the Edge-ID
	 */
	private void handleLogMessageNotification(LogMessageNotification message, String edgeId)
			throws OpenemsNamedException {
		this.logInfo.accept(this.log, "Edge [" + edgeId + "] " //
				+ message.level.getName() + "-Message: " //
				+ message.msg);
	}

	private void announceOnline(String edgeId) {
		var edgeOpt = this.getEdge.apply(edgeId);
		if (edgeOpt.isPresent()) {
			announceOnline(edgeOpt.get());
		}
	}

	protected static void announceOnline(Edge edge) {
		edge.setOnline(true);
		edge.setLastmessage();
	}

	private void announceOffline(String edgeId) {
		var edgeOpt = this.getEdge.apply(edgeId);
		if (edgeOpt.isPresent()) {
			var edge = edgeOpt.get();
			edge.setOnline(false);
		}
	}

}
