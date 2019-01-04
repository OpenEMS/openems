package io.openems.backend.uiwebsocket.impl;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.java_websocket.WebSocket;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.types.ChannelAddress;

public class OnRequest implements io.openems.common.websocket.OnRequest {

//	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final UiWebsocketImpl parent;

	public OnRequest(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		switch (request.getMethod()) {
		case EdgeRpcRequest.METHOD:
			return this.handleEdgeRpcRequest(ws, EdgeRpcRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles an EdgeRpcRequest
	 * 
	 * @param ws             the Websocket
	 * @param edgeRpcRequest the JSON-RPC Request
	 * @return the JSON-RPC Success Response Future
	 * @throws ErrorException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleEdgeRpcRequest(WebSocket ws, EdgeRpcRequest edgeRpcRequest)
			throws OpenemsNamedException {
		String edgeId = edgeRpcRequest.getEdgeId();
		JsonrpcRequest request = edgeRpcRequest.getPayload();

		CompletableFuture<JsonrpcResponseSuccess> resultFuture;
		switch (request.getMethod()) {

		case SubscribeChannelsRequest.METHOD:
			resultFuture = this.handleSubscribeChannelsRequest(ws, edgeId, SubscribeChannelsRequest.from(request));
			break;

		case QueryHistoricTimeseriesDataRequest.METHOD:
			resultFuture = this.handleQueryHistoricDataRequest(edgeId,
					QueryHistoricTimeseriesDataRequest.from(request));
			break;

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}

		// Get Response
		JsonrpcResponseSuccess result;
		try {
			result = resultFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			if (e.getCause() instanceof OpenemsNamedException) {
				throw (OpenemsNamedException) e.getCause();
			} else {
				throw OpenemsError.GENERIC.exception(e.getMessage());
			}
		}

		// Wrap reply in EdgeRpcResponse
		return CompletableFuture.completedFuture(new EdgeRpcResponse(edgeRpcRequest.getId(), result));
	}

	/**
	 * Handles a SubscribeChannelsRequest.
	 * 
	 * @param ws      the Websocket
	 * @param edgeId  the Edge-ID
	 * @param request the SubscribeChannelsRequest
	 * @throws ErrorException on error
	 * @return the JSON-RPC Success Response Future
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WebSocket ws, String edgeId,
			SubscribeChannelsRequest request) throws OpenemsNamedException {
		// activate SubscribedChannelsWorker
		WsData wsData = ws.getAttachment();
		SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
		worker.setEdgeId(edgeId);
		worker.setChannels(request.getChannels());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a QueryHistoricDataRequest.
	 * 
	 * @param ws      the Websocket
	 * @param edgeId  the Edge-ID
	 * @param request the QueryHistoricDataRequest
	 * @throws OpenemsException on error
	 * @return the Future JSON-RPC Response
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(String edgeId,
			QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
		TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> data;
		data = this.parent.timeData.queryHistoricData( //
				edgeId, //
				request.getFromDate(), //
				request.getToDate(), //
				request.getChannels());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new QueryHistoricTimeseriesDataResponse(request.getId(), data));
	}

	//
//	/**
//	 * Query history command
//	 *
//	 * @param j
//	 */
//	private void historicData(WebSocket websocket, JsonObject jMessageId, int edgeId, JsonObject jHistoricData) {
//		try {

//			WebSocketUtils.sendOrLogError(websocket, j);
//			return;
//		} catch (Exception e) {
//			WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
//					Notification.UNABLE_TO_QUERY_HISTORIC_DATA, "Edge [ID:" + edgeId + "] " + e.getMessage());
//		}
//	}

//	/**
//	 * Handles a GetStatusOfEdgesRequest.
//	 * 
//	 * @param jsonrpcRequest
//	 * @param responseCallback
//	 * @throws OpenemsException
//	 */
//	private void handleGetStatusOfEdgesRequest(JsonrpcRequest jsonrpcRequest,
//			Consumer<JsonrpcResponse> responseCallback) throws OpenemsException {
//		GetStatusOfEdgesRequest request = GetStatusOfEdgesRequest.from(jsonrpcRequest);
//		Collection<Edge> edges = this.parent.metadata.getAllEdges();
//		Map<String, EdgeInfo> result = new HashMap<>();
//		for (Edge edge : edges) {
//			EdgeInfo info = new EdgeInfo(edge.isOnline());
//			result.put(edge.getId(), info);
//		}
//		GetStatusOfEdgesResponse response = new GetStatusOfEdgesResponse(request.getId(), result);
//		responseCallback.accept(response);
//	}

//	private void handleCompatibilty(JsonObject jMessage) {
//		// get current User
//		WebsocketData data = websocket.getAttachment();
//		Integer userId = data.getUserId();
//		if (userId == null) {
//			return;
//		}
//		Optional<User> userOpt = this.parent.parent.metadataService.getUser(userId);
//		if (!userOpt.isPresent()) {
//			WebSocketUtils.sendNotificationOrLogError(websocket, new JsonObject(), LogBehaviour.WRITE_TO_LOG,
//					Notification.BACKEND_UNABLE_TO_READ_USER_DETAILS, userId);
//			return;
//		}
//		User user = userOpt.get();
//
//		// get MessageId from message
//		Optional<JsonObject> jMessageIdOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "messageId");
//
//		// get EdgeId from message
//		Optional<Integer> edgeIdOpt = JsonUtils.getAsOptionalInt(jMessage, "edgeId");
//
//		if (jMessageIdOpt.isPresent() && edgeIdOpt.isPresent()) {
//			JsonObject jMessageId = jMessageIdOpt.get();
//			int edgeId = edgeIdOpt.get();
//
//			// get Edge
//			Edge edge;
//			try {
//				edge = this.parent.parent.metadataService.getEdge(edgeId);
//			} catch (OpenemsException e) {
//				WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
//						Notification.BACKEND_UNABLE_TO_READ_EDGE_DETAILS, edgeId, e.getMessage());
//				return;
//			}
//
//			/*
//			 * verify that User is allowed to access Edge
//			 */
//			if (!user.getEdgeRole(edgeId).isPresent()) {
//				WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
//						Notification.BACKEND_FORWARD_TO_EDGE_NOT_ALLOWED, edge.getName(), user.getName());
//				return;
//			}
//
//			/*
//			 * Serve "Config -> Query" from cache
//			 */
//			Optional<JsonObject> jConfigOpt = JsonUtils.getAsOptionalJsonObject(jMessage, "config");
//			if (jConfigOpt.isPresent()) {
//				JsonObject jConfig = jConfigOpt.get();
//				switch (JsonUtils.getAsOptionalString(jConfig, "mode").orElse("")) {
//				case "query":
//					/*
//					 * Query current config
//					 */
//					log.info("User [" + user.getName() + "] queried config for Edge [" + edge.getName() + "]: "
//							+ StringUtils.toShortString(jConfig, 50));
//					JsonObject jReply = DefaultMessages.configQueryReply(jMessageId, edge.getConfig());
//					WebSocketUtils.sendOrLogError(websocket, jReply);
//					return;
//				}
//			}
//
//			/*
//			 * Forward to OpenEMS Edge
//			 */
//			if (jMessage.has("config") || jMessage.has("log") || jMessage.has("system")) {
//				try {
//					log.info("User [" + user.getName() + "] forward message to Edge [" + edge.getName() + "]: "
//							+ StringUtils.toShortString(jMessage, 100));
//					Optional<Role> roleOpt = user.getEdgeRole(edgeId);
//					JsonObject j = DefaultMessages.prepareMessageForForwardToEdge(jMessage, data.getUuid(), roleOpt);
//					this.parent.parent.edgeWebsocketService.forwardMessageFromUi(edgeId, j);
//				} catch (OpenemsException | NoSuchElementException e) {
//					WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
//							Notification.EDGE_UNABLE_TO_FORWARD, edge.getName(), e.getMessage());
//				}
//			}
//		}
//	}

//	/**
//	 * Handle current data subscriptions
//	 *
//	 * @param j
//	 */
//	private synchronized void currentData(WebSocket websocket, WebsocketData data, JsonObject jMessageId, int edgeId,
//			JsonObject jCurrentData) {
//		try {
//			String mode = JsonUtils.getAsString(jCurrentData, "mode");
//
//			if (mode.equals("subscribe")) {
//				/*
//				 * Subscribe to channels
//				 */
//
//				// remove old worker if it existed
//				Optional<BackendCurrentDataWorker> workerOpt = data.getCurrentDataWorker();
//				if (workerOpt.isPresent()) {
//					data.setCurrentDataWorker(null);
//					workerOpt.get().dispose();
//				}
//
//				// set new worker
//				JsonObject jSubscribeChannels = JsonUtils.getAsJsonObject(jCurrentData, "channels");
//				BackendCurrentDataWorker worker = new BackendCurrentDataWorker(this.parent, websocket, edgeId);
//				worker.setChannels(jSubscribeChannels, jMessageId);
//				data.setCurrentDataWorker(worker);
//			}
//		} catch (OpenemsException e) {
//			WebSocketUtils.sendNotificationOrLogError(websocket, jMessageId, LogBehaviour.WRITE_TO_LOG,
//					Notification.SUBSCRIBE_CURRENT_DATA_FAILED, "Edge [ID:" + edgeId + "] " + e.getMessage());
//		}
//	}

}
