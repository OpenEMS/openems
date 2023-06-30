package io.openems.backend.common.uiwebsocket;

import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.backend.common.edgewebsocket.EdgeCache;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

@ProviderType
public interface UiWebsocket {

	/**
	 * Send a JSON-RPC Request to a UI session via WebSocket and expect a JSON-RPC
	 * Response.
	 *
	 * @param token   the UI token
	 * @param request the JsonrpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> send(String token, JsonrpcRequest request)
			throws OpenemsNamedException;

	/**
	 * Send a JSON-RPC Notification to a UI session.
	 *
	 * @param token        the UI token
	 * @param notification the JsonrpcNotification
	 * @throws OpenemsNamedException on error
	 */
	public void send(String token, JsonrpcNotification notification) throws OpenemsNamedException;

	/**
	 * Send a JSON-RPC Notification broadcast to all UI sessions with a given
	 * Edge-ID.
	 *
	 * @param edgeId       the Edge-ID
	 * @param notification the JsonrpcNotification
	 * @throws OpenemsNamedException on error
	 */
	public void sendBroadcast(String edgeId, JsonrpcNotification notification) throws OpenemsNamedException;

	/**
	 * Sends the subscribed Channels to the UI session.
	 * 
	 * @param edgeId    the Edge-ID
	 * @param edgeCache the {@link EdgeCache} for the Edge-ID
	 */
	public void sendSubscribedChannels(String edgeId, EdgeCache edgeCache);

}
