package io.openems.backend.edgewebsocket;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.backend.common.metadata.Edge.Events;
import io.openems.common.channel.Level;
import io.openems.common.event.EventBuilder;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.AbstractDataNotification;
import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.LogMessageNotification;
import io.openems.common.jsonrpc.notification.ResendDataNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final Logger log = LoggerFactory.getLogger(OnNotification.class);
	private final EdgeWebsocketImpl parent;

	public OnNotification(EdgeWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) throws OpenemsNamedException {
		// Validate authentication
		WsData wsData = ws.getAttachment();
		final String edgeId;
		try {
			edgeId = wsData.assertEdgeIdWithTimeout(notification, 5, TimeUnit.SECONDS);
		} catch (OpenemsNamedException e) {
			this.parent.logWarn(this.log, null, e.getMessage());
			return;
		}

		// announce incoming message for this Edge
		this.parent.metadata.getEdge(edgeId).ifPresent(edge -> {
			edge.setLastmessage();
		});

		// Handle notification
		switch (notification.getMethod()) {
		case EdgeConfigNotification.METHOD ->
			this.handleEdgeConfigNotification(EdgeConfigNotification.from(notification), wsData);
		case TimestampedDataNotification.METHOD ->
			this.handleDataNotification(TimestampedDataNotification.from(notification), wsData);
		case AggregatedDataNotification.METHOD ->
			this.handleDataNotification(AggregatedDataNotification.from(notification), wsData);
		case ResendDataNotification.METHOD ->
			this.handleResendDataNotification(ResendDataNotification.from(notification), wsData);
		case SystemLogNotification.METHOD ->
			this.handleSystemLogNotification(SystemLogNotification.from(notification), wsData);
		case LogMessageNotification.METHOD ->
			this.handleLogMessageNotification(LogMessageNotification.from(notification), wsData);
		default -> this.parent.logWarn(this.log, edgeId, "Unhandled Notification: " + notification);
		}
	}

	/**
	 * Handles EdgeConfigNotification.
	 *
	 * @param message the EdgeConfigNotification
	 * @param wsData  the WebSocket attachment
	 * @throws OpenemsException on error
	 */
	private void handleEdgeConfigNotification(EdgeConfigNotification message, WsData wsData) throws OpenemsException {
		var edgeId = wsData.assertEdgeId(message);

		// save config in metadata
		var edge = this.parent.metadata.getEdgeOrError(edgeId);
		EventBuilder.from(this.parent.eventAdmin, Events.ON_SET_CONFIG) //
				.addArg(Events.OnSetConfig.EDGE, edge) //
				.addArg(Events.OnSetConfig.CONFIG, message.getConfig()) //
				.send(); //

		// forward
		try {
			if (this.parent.uiWebsocket != null) {
				this.parent.uiWebsocket.sendBroadcast(edgeId, new EdgeRpcNotification(edgeId, message));
			}
		} catch (NullPointerException e) {
			this.parent.logWarn(this.log, edgeId,
					"Unable to forward EdgeConfigNotification to UI: NullPointerException");
			e.printStackTrace();
		}
	}

	private void handleResendDataNotification(//
			final ResendDataNotification message, //
			final WsData wsData //
	) throws OpenemsNamedException {
		final var edgeId = wsData.assertEdgeId(message);
		this.parent.timedataManager.write(edgeId, message);
	}

	/**
	 * Handles TimestampedDataNotification.
	 *
	 * @param message the TimestampedDataNotification
	 * @param wsData  the WebSocket attachment
	 * @throws OpenemsNamedException on error
	 */
	private void handleDataNotification(AbstractDataNotification message, WsData wsData) throws OpenemsNamedException {
		var edgeId = wsData.assertEdgeId(message);

		try {
			// TODO java 21 switch case with type
			if (message instanceof TimestampedDataNotification timestampNotification) {
				wsData.edgeCache.updateCurrentData(timestampNotification);
				this.parent.timedataManager.write(edgeId, timestampNotification);
			} else if (message instanceof AggregatedDataNotification aggregatedNotification) {
				wsData.edgeCache.updateAggregatedData(aggregatedNotification);
				this.parent.timedataManager.write(edgeId, aggregatedNotification);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		// Forward subscribed Channels to UI
		if (this.parent.uiWebsocket != null) {
			this.parent.uiWebsocket.sendSubscribedChannels(edgeId, wsData.edgeCache);
		}

		// Read some specific channels
		var edge = this.parent.metadata.getEdgeOrError(edgeId);
		for (Entry<String, JsonElement> entry : message.getParams().entrySet()) {
			var d = JsonUtils.getAsJsonObject(entry.getValue());

			// set specific Edge values
			if (d.has("_sum/State") && d.get("_sum/State").isJsonPrimitive()) {
				var sumState = Level.fromJson(d, "_sum/State").orElse(Level.FAULT);
				edge.setSumState(sumState);
			}

			if (d.has("_meta/Version") && d.get("_meta/Version").isJsonPrimitive()) {
				var version = JsonUtils.getAsPrimitive(d, "_meta/Version").getAsString();
				edge.setVersion(SemanticVersion.fromString(version));
			}

		}
	}

	/**
	 * Handles SystemLogNotification.
	 *
	 * @param message the SystemLogNotification
	 * @param wsData  the WebSocket attachment
	 * @throws OpenemsNamedException on error
	 */
	private void handleSystemLogNotification(SystemLogNotification message, WsData wsData)
			throws OpenemsNamedException {
		var edgeId = wsData.assertEdgeId(message);
		this.parent.handleSystemLogNotification(edgeId, message);
	}

	/**
	 * Handles a {@link LogMessageNotification}. Logs given message from request.
	 *
	 * @param notification the {@link LogMessageNotification}
	 * @param wsData       the WebSocket attachment
	 */
	private void handleLogMessageNotification(LogMessageNotification notification, WsData wsData)
			throws OpenemsNamedException {
		this.parent.logInfo(this.log, "Edge [" + wsData.getEdgeId().orElse("NOT AUTHENTICATED") + "] " //
				+ notification.level.getName() + "-Message: " //
				+ notification.msg);
	}

}
