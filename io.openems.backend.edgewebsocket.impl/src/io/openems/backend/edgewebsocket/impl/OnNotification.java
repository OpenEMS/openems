package io.openems.backend.edgewebsocket.impl;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.TimestampedData;

public class OnNotification implements io.openems.common.websocket.OnNotification {

//	private final Logger log = LoggerFactory.getLogger(OnNotification.class);
	private final EdgeWebsocketImpl parent;

	public OnNotification(EdgeWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonrpcNotification notification) throws OpenemsException {
		// Validate authentication
		WsData wsData = ws.getAttachment();
		wsData.assertAuthentication(notification);

		// Handle notification
		switch (notification.getMethod()) {

		case TimestampedData.METHOD:
			this.handleTimestampedData(notification, wsData);
			break;
		}
	}

	private void handleTimestampedData(JsonrpcNotification notification, WsData wsData) throws OpenemsException {
		TimestampedData message = TimestampedData.from(notification);
		String edgeId = wsData.assertEdgeId(notification);

		this.parent.timedata.write(edgeId, notification.getParams());
	}

//	private void timedata(int[] edgeIds, JsonObject jTimedata) {
//	for (int edgeId : edgeIds) {
//		Edge edge;
//		try {
//			edge = this.parent.parent.metadata.getEdge(edgeId);
//		} catch (OpenemsException e) {
//			log.warn(e.getMessage());
//			continue;
//		}
//		/*
//		 * write data to timedataService
//		 */
//		try {
//			this.parent.parent.timedata.write(edgeId, jTimedata);
//			log.debug("Edge [" + edge.getName() + "] wrote " + jTimedata.entrySet().size() + " timestamps "
//					+ StringUtils.toShortString(jTimedata, 120));
//		} catch (Exception e) {
//			log.error("Unable to write Timedata: " + e.getClass().getSimpleName() + ": " + e.getMessage());
//		}
//
//		for (Entry<String, JsonElement> jTimedataEntry : jTimedata.entrySet()) {
//			try {
//				JsonObject jChannels = JsonUtils.getAsJsonObject(jTimedataEntry.getValue());
//				// set Odoo last update timestamp only for those channels
//				for (String channel : jChannels.keySet()) {
//					if (channel.endsWith("ActivePower")
//							|| channel.endsWith("ActivePowerL1") | channel.endsWith("ActivePowerL2")
//									| channel.endsWith("ActivePowerL3") | channel.endsWith("Soc")) {
//						edge.setLastUpdateTimestamp();
//					}
//				}
//
//				// set specific Odoo values
//				if (jChannels.has("ess0/Soc")) {
//					int soc = JsonUtils.getAsPrimitive(jChannels, "ess0/Soc").getAsInt();
//					edge.setSoc(soc);
//				}
//				if (jChannels.has("system0/PrimaryIpAddress")) {
//					String ipv4 = JsonUtils.getAsPrimitive(jChannels, "system0/PrimaryIpAddress").getAsString();
//					edge.setIpv4(ipv4);
//				}
//				if (jChannels.has("_meta/Version")) {
//					String version = JsonUtils.getAsPrimitive(jChannels, "_meta/Version").getAsString();
//					edge.setVersion(version);
//				}
//			} catch (OpenemsException e) {
//				log.error("Edgde [" + edge.getName() + "] error: " + e.getMessage());
//			}
//		}
//	}
}
