package io.openems.backend.edge.manager;

import static io.openems.common.channel.Level.FAULT;

import java.io.Closeable;
import java.io.IOException;
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
import io.openems.common.jsonrpc.notification.ScheduleNotification;
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
	private final BiConsumer<String, ScheduleNotification> handleScheduleNotification;
	private final BiConsumer<Logger, String> logInfo;
	private final BiConsumer<Logger, String> logWarn;

	public OnNotification(//
			String name, //
			Supplier<EventAdmin> eventAdmin, //
			Supplier<UiWebsocket> uiWebsocket, //
			Supplier<TimedataManager> timedataManager, //
			Function<String, Optional<Edge>> getEdge, //
			BiConsumer<String, SystemLogNotification> handleSystemLogNotification, //
			BiConsumer<String, ScheduleNotification> handleScheduleNotification, //
			BiConsumer<Logger, String> logInfo, //
			BiConsumer<Logger, String> logWarn) {
		this.name = name;
		this.eventAdmin = eventAdmin;
		this.uiWebsocket = uiWebsocket;
		this.timedataManager = timedataManager;
		this.getEdge = getEdge;
		this.handleSystemLogNotification = handleSystemLogNotification;
		this.handleScheduleNotification = handleScheduleNotification;
		this.logInfo = logInfo;
		this.logWarn = logWarn;
	}

	protected Closeable startTimer(String method) {
		return PrometheusMetrics.WEBSOCKET_REQUEST.labelValues(this.name, method).startTimer();
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) throws OpenemsNamedException {
		final var method = notification.getMethod();
		try (final var timer = this.startTimer(notification.getFullyQualifiedMethod())) {
			WsData wsData = ws.getAttachment();

			switch (method) {
			case ConnectedEdges.Notification.METHOD //
				-> this.handleConnectedEdgesNotification(ConnectedEdges.Notification.from(notification), wsData);
			case EdgeRpcNotification.METHOD //
				-> this.handleEdgeRpcNotification(EdgeRpcNotification.from(notification), wsData);
			default //
				-> this.logWarn.accept(this.log, "Unhandled Notification: " + notification);
			}
		} catch (IOException e) {
			this.log.debug("Failed to close timer for notification: {}", method, e);
		}
	}

	protected BiConsumer<Double, String> getMetricsConsumer(String metric) {
		final var metricCollector = switch (metric) {
		case "Connections" -> PrometheusMetrics.WEBSOCKET_CONNECTION;
		case "Pending" -> PrometheusMetrics.THREAD_POOL_QUEUE;
		case "Active" -> PrometheusMetrics.THREAD_POOL_ACTIVE_COUNT;
		case "Completed" -> PrometheusMetrics.THREAD_POOL_COMPLETED_TASKS;
		case "PoolSize" -> PrometheusMetrics.THREAD_POOL_CURRENT_SIZE;
		case "MaxPoolSize" -> PrometheusMetrics.THREAD_POOL_MAX_SIZE;
		default -> null;
		};
		if (metricCollector == null) {
			return null;
		}
		return (value, label) -> metricCollector.labelValues(label).set(value);
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
					metrics.getData().cellSet().forEach(cell -> {
						try {
							final var channelAddress = ChannelAddress.fromString(cell.getColumnKey());
							final var metric = this.getMetricsConsumer(channelAddress.getChannelId());
							if (metric != null) {
								final var label = channelAddress.getComponentId();
								final var value = cell.getValue().getAsDouble();
								metric.accept(value, label);
							}
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
		this.getEdge.apply(edgeId).ifPresent(OnNotification::announceOnline);

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
		case ScheduleNotification.METHOD //
			-> this.handleScheduleNotification(ScheduleNotification.from(notification), edgeId);
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
		this.getEdge.apply(edgeId).ifPresent(edge -> {
			EventBuilder.from(this.eventAdmin.get(), Events.ON_SET_CONFIG) //
					.addArg(Events.OnSetConfig.EDGE, edge) //
					.addArg(Events.OnSetConfig.CONFIG, message.getConfig()) //
					.send(); //
		});

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
		final var edgeClient = wsData.getEdgeClient(edgeId);
		if (edgeClient == null) {
			return;
		}

		final var edgeCache = edgeClient.getCache();
		final var timedataManager = this.timedataManager.get();
		if (timedataManager != null) {
			switch (message) {
			case TimestampedDataNotification notification -> {
				edgeCache.updateCurrentData(notification);
				timedataManager.write(edgeId, notification);
			}
			case AggregatedDataNotification notification -> {
				edgeCache.updateAggregatedData(notification);
				timedataManager.write(edgeId, notification);
			}
			case ResendDataNotification unused -> {
				// Nothing
			}
			}
		}

		// Forward subscribed Channels to UI
		final var uiWebsocket = this.uiWebsocket.get();
		if (uiWebsocket != null) {
			uiWebsocket.sendSubscribedChannels(edgeId, edgeCache);
		}

		// Read some specific channels
		this.getEdge.apply(edgeId).ifPresent(edge -> {
			final var data = message.getData();
			data.getLatestColumnValue("_sum/State") //
					.map(json -> Level.fromJson(json).orElse(FAULT)) //
					.ifPresent(edge::setSumState);
			data.getLatestColumnValue("_meta/Version") //
					.map(json -> SemanticVersion.fromString(json.getAsString())) //
					.ifPresent(edge::setVersion);
		});
	}

	/**
	 * Handles a {@link ResendDataNotification}.
	 *
	 * @param message the {@link ResendDataNotification}
	 * @param edgeId  the Edge-ID
	 */
	private void handleResendDataNotification(ResendDataNotification message, String edgeId) {
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
	private void handleSystemLogNotification(SystemLogNotification message, String edgeId) {
		this.handleSystemLogNotification.accept(edgeId, message);
	}

	/**
	 * Handles a {@link ScheduleNotification}.
	 *
	 * @param message the {@link ScheduleNotification}
	 * @param edgeId  the Edge-ID
	 */
	private void handleScheduleNotification(ScheduleNotification message, String edgeId) {
		this.handleScheduleNotification.accept(edgeId, message);
	}

	/**
	 * Handles a {@link LogMessageNotification}. Logs given message from request.
	 *
	 * @param message the {@link LogMessageNotification}
	 * @param edgeId  the Edge-ID
	 */
	private void handleLogMessageNotification(LogMessageNotification message, String edgeId) {
		this.logInfo.accept(this.log, "Edge [" + edgeId + "] " //
				+ message.level.getName() + "-Message: " //
				+ message.msg);
	}

	private void announceOnline(String edgeId) {
		this.getEdge.apply(edgeId).ifPresent(OnNotification::announceOnline);
	}

	protected static void announceOnline(Edge edge) {
		edge.setOnline(true);
		edge.setLastmessage();
	}

	private void announceOffline(String edgeId) {
		this.getEdge.apply(edgeId).ifPresent(OnNotification::announceOffline);
	}

	protected static void announceOffline(Edge edge) {
		edge.setOnline(false);
	}

}
