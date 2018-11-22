package io.openems.backend.edgewebsocket.api;

import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

@ProviderType
public interface EdgeWebsocket {

	/**
	 * Send a JSON-RPC Request to an Edge via Websocket and expect a JSON-RPC
	 * Response
	 * 
	 * @param edgeId
	 * @param request
	 * @param responseCallback
	 */
	public void send(String edgeId, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback);

	/**
	 * Send a JSON-RPC Notification to an Edge
	 * 
	 * @param edgeId
	 * @param request
	 * @param responseCallback
	 */
	public void send(String edgeId, JsonrpcNotification notification);

}
