package io.openems.edge.controller.api.backend;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final BackendApi parent;

	public OnRequest(BackendApi parent) {
		this.parent = parent;
	}

	@Override
	public void run(WebSocket ws, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback)
			throws OpenemsException {
		System.out.println("BackendApi onRequest " + request);

//		switch (request.getMethod()) {
//
//		case GetStatusOfEdgesRequest.METHOD:
//			this.handleGetStatusOfEdgesRequest(request, responseCallback);
//			break;
//
//		case SetGridConnScheduleRequest.METHOD:
//			this.handleSetGridConnScheduleRequest(request, responseCallback);
//
//		}
	}

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

}
