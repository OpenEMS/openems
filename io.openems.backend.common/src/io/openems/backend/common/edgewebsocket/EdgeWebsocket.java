package io.openems.backend.common.edgewebsocket;

import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.types.ChannelAddress;

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
	 * @param edgeId      the Edge-ID
	 * @param user        the {@link User}
	 * @param websocketId the id of the UI websocket connection
	 * @param request     the {@link SubscribeSystemLogRequest}
	 * @return a reply
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(String edgeId, User user,
			UUID websocketId, SubscribeSystemLogRequest request) throws OpenemsNamedException;

	/**
	 * Gets the latest values for the given ChannelAddresses.
	 *
	 * @param edgeId           The unique Edge-ID
	 * @param channelAddresses The {@link ChannelAddress}es
	 * @return the values; possibly {@link JsonNull}
	 */
	public SortedMap<ChannelAddress, JsonElement> getChannelValues(String edgeId, Set<ChannelAddress> channelAddresses);

}
