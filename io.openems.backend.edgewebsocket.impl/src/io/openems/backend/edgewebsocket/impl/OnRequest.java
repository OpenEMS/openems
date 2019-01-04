package io.openems.backend.edgewebsocket.impl;

import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final EdgeWebsocketImpl parent;

	public OnRequest(EdgeWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		log.info("EdgeWs. OnRequest: " + request);
		return null;
	}

//	private void handleCompatibilty(JsonObject jMessage) {
//		// set last update timestamps in MetadataService
//		for (int edgeId : edgeIds) {
//			Optional<Edge> edgeOpt = this.parent.parent.metadata.getEdgeOpt(edgeId);
//			if (edgeOpt.isPresent()) {
//				Edge edge = edgeOpt.get();
//				edge.setLastMessageTimestamp();
//			}
//		}
//
//		// get MessageId from message
//		JsonObject jMessageId = JsonUtils.getAsOptionalJsonObject(jMessage, "messageId").orElse(new JsonObject());
//
//		/*
//		 * Config? -> store in Metadata
//		 */
//		Optional<JsonObject> jConfigOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
//		if (jConfigOpt.isPresent()) {
//			JsonObject jConfig = jConfigOpt.get();
//			for (int edgeId : edgeIds) {
//				Edge edge;
//				try {
//					edge = this.parent.parent.metadata.getEdge(edgeId);
//					edge.setConfig(jConfig);
//				} catch (OpenemsException e) {
//					WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
//							Notification.METADATA_ERROR, e.getMessage());
//				}
//			}
//			return;
//		}
//
//		/*
//		 * Is this a reply? -> forward to UI
//		 */
//		if (jMessage.has("messageId")) {
//			for (int edgeId : edgeIds) {
//				try {
//					this.parent.parent.uiWebsocket.handleEdgeReply(edgeId, jMessage);
//				} catch (OpenemsException e) {
//					WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
//							Notification.EDGE_UNABLE_TO_FORWARD, "ID:" + edgeId, e.getMessage());
//				}
//			}
//			return;
//		}
//
//		/*
//		 * New timestamped data
//		 */
//		Optional<JsonObject> jTimedataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "timedata");
//		if (jTimedataOpt.isPresent()) {
//			timedata(edgeIds, jTimedataOpt.get());
//			return;
//		}
//
//		/*
//		 * Unknown message
//		 */
//		for (String edgeName : this.parent.getEdgeNames(edgeIds)) {
//			WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
//					Notification.UNKNOWN_MESSAGE, edgeName, StringUtils.toShortString(jMessage, 100));
//		}
//	}
//

}
