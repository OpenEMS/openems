package io.openems.backend.b2bwebsocket;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.WsData;
import io.openems.backend.b2bwebsocket.jsonrpc.GetStatusOfEdgesRequest;
import io.openems.backend.b2bwebsocket.jsonrpc.GetStatusOfEdgesResponse;
import io.openems.backend.b2bwebsocket.jsonrpc.GetStatusOfEdgesResponse.EdgeInfo;
import io.openems.backend.metadata.api.Edge;

public class WebsocketServer extends AbstractWebsocketServer {

	private final Logger log = LoggerFactory.getLogger(WebsocketServer.class);
	private final B2bWebsocket parent;

	public WebsocketServer(B2bWebsocket parent, String name, int port) {
		super(name, port);
		this.parent = parent;
	}

	@Override
	protected WsData onOpen(WebSocket ws, JsonObject handshake) {
		B2bWsData wsData = new B2bWsData();
		return wsData;
	}

	@Override
	protected void onRequest(WebSocket ws, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback)
			throws OpenemsException {
		switch (request.getMethod()) {

		case GetStatusOfEdgesRequest.METHOD:
			this.handleGetStatusOfEdgesRequest(request, responseCallback);
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
		Collection<Edge> edges = this.parent.metadataService.getAllEdges();
		Map<String, EdgeInfo> result = new HashMap<>();
		for (Edge edge : edges) {
			EdgeInfo info = new EdgeInfo(edge.isOnline());
			result.put(edge.getName(), info);
		}
		GetStatusOfEdgesResponse response = new GetStatusOfEdgesResponse(request.getId(), result);
		responseCallback.accept(response);
	}

	@Override
	protected void onError(WebSocket ws, Exception ex) {
		log.info("OnError: " + ex.getMessage());
		// TODO Auto-generated method stub
	}

	@Override
	protected void onClose(WebSocket ws, int code, String reason, boolean remote) {
		log.info("OnClose: " + reason);
		// TODO Auto-generated method stub
	}

	@Override
	protected void onInternalError(Exception ex) {
		log.info("OnInternalError: " + ex.getMessage());
		ex.printStackTrace();
	}

}
