package io.openems.backend.edgewebsocket.impl;

import java.util.Map.Entry;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.common.websocket.AbstractOnMessage;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;

public class OnMessage extends AbstractOnMessage {

	private final Logger log = LoggerFactory.getLogger(OnMessage.class);
	private final EdgeWebsocketServer parent;

	public OnMessage(EdgeWebsocketServer parent, WebSocket websocket, String message) {
		super(websocket, message);
		this.parent = parent;
	}

	protected void run(WebSocket websocket, String message) {
//		TODO implement JSON-RPC
//		try {
//			JsonrpcResponse response = null;
//			JsonrpcRequest request = JsonrpcRequest.from(message);
//			try {
//				/*
//				 * Handle JsonrpcRequest
//				 */
//				switch (request.getMethod()) {
//				// TODO
//				}
//
//				/*
//				 * Reply with JsonrpcResponse
//				 */
//				// 
//				if (response != null) {
//					WebSocketUtils.send(this.websocket, response.toString());
//				}
//
//			} catch (OpenemsException e) {
//				log.error("Unable to handle message: " + e.getMessage());
//			}
//
//		} catch (JSONException e) {
		/*
		 * Handle Compatibility for pre-JSONRPC-Requests
		 */
		this.handleCompatibilty((new JsonParser()).parse(message).getAsJsonObject());
//		}
	}

	private void handleCompatibilty(JsonObject jMessage) {
		Attachment attachment = websocket.getAttachment();
		int[] edgeIds = attachment.getEdgeIds();
		if (edgeIds.length == 0) {
			log.warn("Websocket was not fully handled by OnOpen yet. Apikey [" + attachment.getApikey() + "]");
			this.parent.executorTryAgain(this);
		}

		// set last update timestamps in MetadataService
		for (int edgeId : edgeIds) {
			Optional<Edge> edgeOpt = this.parent.parent.metadataService.getEdgeOpt(edgeId);
			if (edgeOpt.isPresent()) {
				Edge edge = edgeOpt.get();
				edge.setLastMessage();
			}
		}

		// get MessageId from message
		JsonObject jMessageId = JsonUtils.getAsOptionalJsonObject(jMessage, "messageId").orElse(new JsonObject());

		/*
		 * Config? -> store in Metadata
		 */
		Optional<JsonObject> jConfigOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
		if (jConfigOpt.isPresent()) {
			JsonObject jConfig = jConfigOpt.get();
			for (int edgeId : edgeIds) {
				Edge edge;
				try {
					edge = this.parent.parent.metadataService.getEdge(edgeId);
					edge.setConfig(jConfig);
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
							Notification.METADATA_ERROR, e.getMessage());
				}
			}
			return;
		}

		/*
		 * Is this a reply? -> forward to UI
		 */
		if (jMessage.has("messageId")) {
			for (int edgeId : edgeIds) {
				try {
					this.parent.parent.uiWebsocketService.handleEdgeReply(edgeId, jMessage);
				} catch (OpenemsException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
							Notification.EDGE_UNABLE_TO_FORWARD, "ID:" + edgeId, e.getMessage());
				}
			}
			return;
		}

		/*
		 * New timestamped data
		 */
		Optional<JsonObject> jTimedataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "timedata");
		if (jTimedataOpt.isPresent()) {
			timedata(edgeIds, jTimedataOpt.get());
			return;
		}

		/*
		 * Unknown message
		 */
		for (String edgeName : this.parent.getEdgeNames(edgeIds)) {
			WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
					Notification.UNKNOWN_MESSAGE, edgeName, StringUtils.toShortString(jMessage, 100));
		}
	}

	private void timedata(int[] edgeIds, JsonObject jTimedata) {
		for (int edgeId : edgeIds) {
			Edge edge;
			try {
				edge = this.parent.parent.metadataService.getEdge(edgeId);
			} catch (OpenemsException e) {
				log.warn(e.getMessage());
				continue;
			}
			/*
			 * write data to timedataService
			 */
			try {
				this.parent.parent.timedataService.write(edgeId, jTimedata);
				log.debug("Edge [" + edge.getName() + "] wrote " + jTimedata.entrySet().size() + " timestamps "
						+ StringUtils.toShortString(jTimedata, 120));
			} catch (Exception e) {
				log.error("Unable to write Timedata: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			}

			for (Entry<String, JsonElement> jTimedataEntry : jTimedata.entrySet()) {
				try {
					JsonObject jChannels = JsonUtils.getAsJsonObject(jTimedataEntry.getValue());
					// set Odoo last update timestamp only for those channels
					for (String channel : jChannels.keySet()) {
						if (channel.endsWith("ActivePower")
								|| channel.endsWith("ActivePowerL1") | channel.endsWith("ActivePowerL2")
										| channel.endsWith("ActivePowerL3") | channel.endsWith("Soc")) {
							edge.setLastUpdate();
						}
					}

					// set specific Odoo values
					if (jChannels.has("ess0/Soc")) {
						int soc = JsonUtils.getAsPrimitive(jChannels, "ess0/Soc").getAsInt();
						edge.setSoc(soc);
					}
					if (jChannels.has("system0/PrimaryIpAddress")) {
						String ipv4 = JsonUtils.getAsPrimitive(jChannels, "system0/PrimaryIpAddress").getAsString();
						edge.setIpv4(ipv4);
					}
					if (jChannels.has("_meta/Version")) {
						String version = JsonUtils.getAsPrimitive(jChannels, "_meta/Version").getAsString();
						edge.setVersion(version);
					}
				} catch (OpenemsException e) {
					log.error("Edgde [" + edge.getName() + "] error: " + e.getMessage());
				}
			}
		}
	}

}
