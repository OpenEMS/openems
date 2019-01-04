package io.openems.backend.edgewebsocket.api;

import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

@ProviderType
public interface EdgeWebsocket {

	/**
	 * Send a JSON-RPC Request to an Edge via Websocket and expect a JSON-RPC
	 * Response
	 * 
	 * @param edgeId  the Edge-ID
	 * @param request the JSON-RPC Request
	 * @throws OpenemsNamedException on error
	 * @return the JSON-RPC Success Response Future
	 */
	public CompletableFuture<JsonrpcResponseSuccess> send(String edgeId, JsonrpcRequest request)
			throws OpenemsNamedException;

	/**
	 * Send a JSON-RPC Notification to an Edge
	 * 
	 * @param edgeId  the Edge-ID
	 * @param request the JSON-RPC Notification
	 * @throws OpenemsNamedException on error
	 */
	public void send(String edgeId, JsonrpcNotification notification) throws OpenemsNamedException;

}
