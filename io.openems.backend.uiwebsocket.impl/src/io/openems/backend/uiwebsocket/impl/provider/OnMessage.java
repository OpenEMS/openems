package io.openems.backend.uiwebsocket.impl.provider;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.timedata.Tag;
import io.openems.common.timedata.TimedataUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.common.websocket.AbstractOnMessage;
import io.openems.common.websocket.DefaultMessages;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;

public class OnMessage extends AbstractOnMessage {

	private final Logger log = LoggerFactory.getLogger(OnMessage.class);
	private final UiWebsocketServer parent;

	public OnMessage(UiWebsocketServer parent, WebSocket websocket, String message) {
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
//				}
//
//				/*
//				 * Reply with JsonrpcResponse
//				 */
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
		// get current User
		WebsocketData data = websocket.getAttachment();
		Integer userId = data.getUserId();
		if (userId == null) {
			return;
		}
		Optional<User> userOpt = this.parent.parent.metadataService.getUser(userId);
		if (!userOpt.isPresent()) {
			WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject(), LogBehaviour.WRITE_TO_LOG,
					Notification.BACKEND_UNABLE_TO_READ_USER_DETAILS, userId);
			return;
		}
		User user = userOpt.get();

		// get MessageId from message
		Optional<JsonObject> jMessageIdOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "messageId");

		// get EdgeId from message
		Optional<Integer> edgeIdOpt = JsonUtils.getAsOptionalInt(jMessage, "edgeId");

		if (jMessageIdOpt.isPresent() && edgeIdOpt.isPresent()) {
			JsonObject jMessageId = jMessageIdOpt.get();
			int edgeId = edgeIdOpt.get();

			// get Edge
			Edge edge;
			try {
				edge = this.parent.parent.metadataService.getEdge(edgeId);
			} catch (OpenemsException e) {
				WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
						Notification.BACKEND_UNABLE_TO_READ_EDGE_DETAILS, edgeId, e.getMessage());
				return;
			}

			/*
			 * verify that User is allowed to access Edge
			 */
			if (!user.getEdgeRole(edgeId).isPresent()) {
				WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
						Notification.BACKEND_FORWARD_TO_EDGE_NOT_ALLOWED, edge.getName(), user.getName());
				return;
			}

			/*
			 * Query historic data
			 */
			Optional<JsonObject> jHistoricDataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "historicData");
			if (jHistoricDataOpt.isPresent()) {
				JsonObject jHistoricData = jHistoricDataOpt.get();
				log.info("User [" + user.getName() + "] queried historic data for Edge [" + edge.getName() + "]: "
						+ StringUtils.toShortString(jHistoricData, 50));
				this.historicData(websocket, jMessageId, edgeId, jHistoricData);
				return;
			}

			/*
			 * Subscribe to currentData
			 */
			Optional<JsonObject> jCurrentDataOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "currentData");
			if (jCurrentDataOpt.isPresent()) {
				JsonObject jCurrentData = jCurrentDataOpt.get();
				log.info("User [" + user.getName() + "] subscribed to current data for Edge [" + edge.getName() + "]: "
						+ StringUtils.toShortString(jCurrentData, 50));
				this.currentData(websocket, data, jMessageId, edgeId, jCurrentData);
				return;
			}

			/*
			 * Serve "Config -> Query" from cache
			 */
			Optional<JsonObject> jConfigOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
			if (jConfigOpt.isPresent()) {
				JsonObject jConfig = jConfigOpt.get();
				switch (JsonUtils.getAsOptionalString(jConfig, "mode").orElse("")) {
				case "query":
					/*
					 * Query current config
					 */
					log.info("User [" + user.getName() + "] queried config for Edge [" + edge.getName() + "]: "
							+ StringUtils.toShortString(jConfig, 50));
					JsonObject jReply = DefaultMessages.configQueryReply(jMessageId, edge.getConfig());
					WebSocketUtils.sendOrLogError(websocket, jReply);
					return;
				}
			}

			/*
			 * Forward to OpenEMS Edge
			 */
			if (jMessage.has("config") || jMessage.has("log") || jMessage.has("system")) {
				try {
					log.info("User [" + user.getName() + "] forward message to Edge [" + edge.getName() + "]: "
							+ StringUtils.toShortString(jMessage, 100));
					Optional<Role> roleOpt = user.getEdgeRole(edgeId);
					JsonObject j = DefaultMessages.prepareMessageForForwardToEdge(jMessage, data.getUuid(), roleOpt);
					this.parent.parent.edgeWebsocketService.forwardMessageFromUi(edgeId, j);
				} catch (OpenemsException | NoSuchElementException e) {
					WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
							Notification.EDGE_UNABLE_TO_FORWARD, edge.getName(), e.getMessage());
				}
			}
		}
	}

	/**
	 * Handle current data subscriptions
	 *
	 * @param j
	 */
	private synchronized void currentData(WebSocket websocket, WebsocketData data, JsonObject jMessageId, int edgeId,
			JsonObject jCurrentData) {
		try {
			String mode = JsonUtils.getAsString(jCurrentData, "mode");

			if (mode.equals("subscribe")) {
				/*
				 * Subscribe to channels
				 */

				// remove old worker if it existed
				Optional<BackendCurrentDataWorker> workerOpt = data.getCurrentDataWorker();
				if (workerOpt.isPresent()) {
					data.setCurrentDataWorker(null);
					workerOpt.get().dispose();
				}

				// set new worker
				JsonObject jSubscribeChannels = JsonUtils.getAsJsonObject(jCurrentData, "channels");
				BackendCurrentDataWorker worker = new BackendCurrentDataWorker(this.parent, websocket, edgeId);
				worker.setChannels(jSubscribeChannels, jMessageId);
				data.setCurrentDataWorker(worker);
			}
		} catch (OpenemsException e) {
			WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
					Notification.SUBSCRIBE_CURRENT_DATA_FAILED, "Edge [ID:" + edgeId + "] " + e.getMessage());
		}
	}

	/**
	 * Query history command
	 *
	 * @param j
	 */
	private void historicData(WebSocket websocket, JsonObject jMessageId, int edgeId, JsonObject jHistoricData) {
		try {
			Edge edge = this.parent.parent.metadataService.getEdge(edgeId);
			Tag[] tags = new Tag[] { new Tag("fems", TimedataUtils.parseNumberFromName(edge.getName())) };
			JsonObject j = TimedataUtils.handle(this.parent.parent.timeDataService, jMessageId, jHistoricData, tags);
			WebSocketUtils.sendOrLogError(websocket, j);
			return;
		} catch (Exception e) {
			WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
					Notification.UNABLE_TO_QUERY_HISTORIC_DATA, "Edge [ID:" + edgeId + "] " + e.getMessage());
		}
	}

}
