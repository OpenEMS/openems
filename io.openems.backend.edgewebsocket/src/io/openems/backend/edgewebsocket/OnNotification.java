package io.openems.backend.edgewebsocket;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
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
	public void run(WebSocket ws, JsonrpcNotification notification) throws OpenemsNamedException {
		// Validate authentication
		WsData wsData = ws.getAttachment();
		try {
			wsData.assertAuthenticatedWithTimeout(notification, 5, TimeUnit.SECONDS);
		} catch (OpenemsNamedException e) {
			this.parent.logWarn(this.log, e.getMessage());
			return;
		}

		// announce incoming message for this Edge
		var edgeOpt = wsData.getEdge(this.parent.metadata);
		if (edgeOpt.isPresent()) {
			edgeOpt.get().setLastMessageTimestamp();
		}

		// Handle notification
		switch (notification.getMethod()) {
		case EdgeConfigNotification.METHOD:
			this.handleEdgeConfigNotification(EdgeConfigNotification.from(notification), wsData);
			return;

		case TimestampedDataNotification.METHOD:
			this.handleTimestampedDataNotification(TimestampedDataNotification.from(notification), wsData);
			return;

		case SystemLogNotification.METHOD:
			this.handleSystemLogNotification(SystemLogNotification.from(notification), wsData);
			return;
		}

		this.parent.logWarn(this.log, "Unhandled Notification: " + notification);
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
		edge.setConfig(message.getConfig());

		// forward
		try {
			this.parent.uiWebsocket.sendBroadcast(edgeId, new EdgeRpcNotification(edgeId, message));
		} catch (OpenemsNamedException e) {
			this.parent.logWarn(this.log, "Unable to forward EdgeConfigNotification to UI: " + e.getMessage());
		} catch (NullPointerException e) {
			this.parent.logWarn(this.log, "Unable to forward EdgeConfigNotification to UI: NullPointerException");
			e.printStackTrace();
		}
	}

	/**
	 * Handles TimestampedDataNotification.
	 *
	 * @param message the TimestampedDataNotification
	 * @param wsData  the WebSocket attachment
	 * @throws OpenemsNamedException on error
	 */
	private void handleTimestampedDataNotification(TimestampedDataNotification message, WsData wsData)
			throws OpenemsNamedException {
		var edgeId = wsData.assertEdgeId(message);

		try {
			this.parent.timedata.write(edgeId, message.getData());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		// Read some specific channels
		var edge = this.parent.metadata.getEdgeOrError(edgeId);
		for (Entry<String, JsonElement> entry : message.getParams().entrySet()) {
			var data = JsonUtils.getAsJsonObject(entry.getValue());
			// set Edge last update timestamp only for those channels
			for (String channel : data.keySet()) {
				if (channel.endsWith("ActivePower")
						|| channel.endsWith("ActivePowerL1") | channel.endsWith("ActivePowerL2")
								| channel.endsWith("ActivePowerL3") | channel.endsWith("Soc")) {
					edge.setLastUpdateTimestamp();
				}
			}

			// set specific Edge values
			if (data.has("_sum/State") && data.get("_sum/State").isJsonPrimitive()) {
				var sumState = Level.fromJson(data, "_sum/State").orElse(Level.FAULT);
				edge.setSumState(sumState);
			}

			if (data.has("_meta/Version") && data.get("_meta/Version").isJsonPrimitive()) {
				var version = JsonUtils.getAsPrimitive(data, "_meta/Version").getAsString();
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
}
