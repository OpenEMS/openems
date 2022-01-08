package io.openems.backend.common.edgewebsocket;

import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;

@ProviderType
public interface EdgeWebsocket {

	/**
	 * Send an authenticated JSON-RPC Request to an Edge via Websocket and expect a
	 * JSON-RPC Response.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the authenticated {@link User}
	 * @param request the {@link JsonrpcRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> send(String edgeId, User user, JsonrpcRequest request)
			throws OpenemsNamedException;

	/**
	 * Send a JSON-RPC Notification to an Edge.
	 *
	 * @param edgeId       the Edge-ID
	 * @param notification the JsonrpcNotification
	 * @throws OpenemsNamedException on error
	 */
	public void send(String edgeId, JsonrpcNotification notification) throws OpenemsNamedException;

	/**
	 * Handles a {@link SubscribeSystemLogRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User}
	 * @param token   the UI session token
	 * @param request the {@link SubscribeSystemLogRequest}
	 * @return a reply
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(String edgeId, User user,
			String token, SubscribeSystemLogRequest request) throws OpenemsNamedException;

}
