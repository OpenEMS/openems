package io.openems.backend.b2bwebsocket;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;

import io.openems.backend.metadata.api.Edge;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
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
	public void run(WebSocket ws, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback)
			throws OpenemsException {
		switch (request.getMethod()) {

		case GetStatusOfEdgesRequest.METHOD:
			this.handleGetStatusOfEdgesRequest(request, responseCallback);
			break;

		case SetGridConnScheduleRequest.METHOD:
			this.handleSetGridConnScheduleRequest(request, responseCallback);
			break;
		}
	}

	/**
	 * Handles a GetStatusOfEdgesRequest.
	 * 
	 * @param jsonrpcRequest
	 * @param responseCallback
	 * @throws OpenemsException
	 */
	private void handleGetStatusOfEdgesRequest(JsonrpcRequest jsonrpcRequest,
			Consumer<JsonrpcResponse> responseCallback) throws OpenemsException {
		GetStatusOfEdgesRequest request = GetStatusOfEdgesRequest.from(jsonrpcRequest);
		Collection<Edge> edges = this.parent.metadata.getAllEdges();
		Map<String, EdgeInfo> result = new HashMap<>();
		for (Edge edge : edges) {
			EdgeInfo info = new EdgeInfo(edge.isOnline());
			result.put(edge.getId(), info);
		}
		GetStatusOfEdgesResponse response = new GetStatusOfEdgesResponse(request.getId(), result);
		responseCallback.accept(response);
	}

	/**
	 * Handles a SetGridConnScheduleRequest.
	 * 
	 * @param jsonrpcRequest
	 * @param responseCallback
	 * @throws OpenemsException
	 */
	private void handleSetGridConnScheduleRequest(JsonrpcRequest jsonrpcRequest,
			Consumer<JsonrpcResponse> responseCallback) throws OpenemsException {
		SetGridConnScheduleRequest setGridConnScheduleRequest = SetGridConnScheduleRequest.from(jsonrpcRequest);
		ComponentJsonApiRequest request = new ComponentJsonApiRequest("ctrlGridConnSchedule",
				setGridConnScheduleRequest);
		this.parent.edgeWebsocket.send(setGridConnScheduleRequest.getEdgeId(), request, responseCallback);
	}

}
