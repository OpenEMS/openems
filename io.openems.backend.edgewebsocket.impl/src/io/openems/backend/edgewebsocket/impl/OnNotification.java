package io.openems.backend.edgewebsocket.impl;

import java.util.Map.Entry;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
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
			wsData.assertAuthentication(notification);
		} catch (OpenemsNamedException e) {
			this.parent.logWarn(this.log, e.getMessage());
		}

		// Handle notification
		switch (notification.getMethod()) {
		case EdgeConfigNotification.METHOD:
			this.handleEdgeConfiguration(EdgeConfigNotification.from(notification), wsData);
			return;

		case TimestampedDataNotification.METHOD:
			this.handleTimestampedData(TimestampedDataNotification.from(notification), wsData);
			return;
		}

		this.parent.logWarn(this.log, "Unhandled Notification: " + notification);
	}

	/**
	 * Handle JSON-RPC Notification 'edgeConfiguration'.
	 * 
	 * @param message the EdgeConfigNotification
	 * @param wsData  the WebSocket attachment
	 * @throws OpenemsNamedException on error
	 */
	private void handleEdgeConfiguration(EdgeConfigNotification message, WsData wsData) throws OpenemsNamedException {
		String edgeId = wsData.assertEdgeId(message);
		Edge edge = this.parent.metadata.getEdgeOrError(edgeId);
		edge.setConfig(message.getConfig());
	}

	/**
	 * Handle JSON-RPC Notification 'timestampedData'.
	 * 
	 * @param message the TimestampedDataNotification
	 * @param wsData  the WebSocket attachment
	 * @throws OpenemsNamedException on error
	 */
	private void handleTimestampedData(TimestampedDataNotification message, WsData wsData)
			throws OpenemsNamedException {
		String edgeId = wsData.assertEdgeId(message);

		try {
			this.parent.timedata.write(edgeId, message.getData());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		// Read some specific channels
		Edge edge = this.parent.metadata.getEdgeOrError(edgeId);
		for (Entry<String, JsonElement> entry : message.getParams().entrySet()) {
			JsonObject data = JsonUtils.getAsJsonObject(entry.getValue());
			// set Edge last update timestamp only for those channels
			for (String channel : data.keySet()) {
				if (channel.endsWith("ActivePower")
						|| channel.endsWith("ActivePowerL1") | channel.endsWith("ActivePowerL2")
								| channel.endsWith("ActivePowerL3") | channel.endsWith("Soc")) {
					edge.setLastUpdateTimestamp();
				}
			}

			// set specific Edge values
			if (data.has("ess0/Soc") && data.get("ess0/Soc").isJsonPrimitive()) {
				int soc = JsonUtils.getAsPrimitive(data, "ess0/Soc").getAsInt();
				edge.setSoc(soc);
			}
			if (data.has("system0/PrimaryIpAddress") && data.get("system0/PrimaryIpAddress").isJsonPrimitive()) {
				String ipv4 = JsonUtils.getAsPrimitive(data, "system0/PrimaryIpAddress").getAsString();
				edge.setIpv4(ipv4);
			}
			if (data.has("_meta/Version") && data.get("_meta/Version").isJsonPrimitive()) {
				String version = JsonUtils.getAsPrimitive(data, "_meta/Version").getAsString();
				edge.setVersion(version);
			}
		}
	}

}
