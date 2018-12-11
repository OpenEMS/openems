package io.openems.backend.b2bwebsocket;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.GetStatusOfEdgesRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.response.GetStatusOfEdgesResponse;
import io.openems.common.jsonrpc.response.GetStatusOfEdgesResponse.EdgeInfo;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final B2bWebsocket parent;

	public OnRequest(B2bWebsocket parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback) {
		try {
			switch (request.getMethod()) {

			case GetStatusOfEdgesRequest.METHOD:
				this.handleGetStatusOfEdgesRequest(request.getId(), GetStatusOfEdgesRequest.from(request),
						responseCallback);
				break;

			case SetGridConnScheduleRequest.METHOD:
				this.handleSetGridConnScheduleRequest(request.getId(), SetGridConnScheduleRequest.from(request),
						responseCallback);
				break;

			}
		} catch (OpenemsException e) {
			responseCallback.accept(
					new JsonrpcResponseError(request.getId(), 0, "Error while handling request: " + e.getMessage()));
		}
	}

	/**
	 * Handles a GetStatusOfEdgesRequest.
	 * 
	 * @param jsonrpcRequest
	 * @param responseCallback
	 * @throws OpenemsException
	 */
	private void handleGetStatusOfEdgesRequest(UUID messageId, GetStatusOfEdgesRequest request,
			Consumer<JsonrpcResponse> responseCallback) throws OpenemsException {
		Collection<Edge> edges = this.parent.metadata.getAllEdges();
		Map<String, EdgeInfo> result = new HashMap<>();
		for (Edge edge : edges) {
			EdgeInfo info = new EdgeInfo(edge.isOnline());
			result.put(edge.getId(), info);
		}
		GetStatusOfEdgesResponse response = new GetStatusOfEdgesResponse(messageId, result);
		responseCallback.accept(response);
	}

	/**
	 * Handles a SetGridConnScheduleRequest.
	 * 
	 * @param jsonrpcRequest
	 * @param responseCallback
	 * @throws OpenemsException
	 */
	private void handleSetGridConnScheduleRequest(UUID messageId, SetGridConnScheduleRequest setGridConnScheduleRequest,
			Consumer<JsonrpcResponse> responseCallback) throws OpenemsException {
		// wrap original request inside ComponentJsonApiRequest
		String componentId = "ctrlBalancingSchedule0"; // TODO find dynamic Component-ID of BalancingScheduleController
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, setGridConnScheduleRequest);

		this.parent.edgeWebsocket.send(setGridConnScheduleRequest.getEdgeId(), request, response -> {
			// wrap response with original JSON-RPC id
			JsonrpcResponse wrappedResponse = new GenericJsonrpcResponseSuccess(messageId, response.toJsonObject());
			responseCallback.accept(wrappedResponse);
		});
	}

}
