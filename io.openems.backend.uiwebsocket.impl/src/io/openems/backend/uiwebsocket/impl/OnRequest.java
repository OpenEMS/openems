package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;

import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.session.Role;
import io.openems.common.session.User;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final UiWebsocketImpl parent;

	public OnRequest(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		WsData wsData = ws.getAttachment();
		BackendUser user = this.assertUser(wsData, request);

		CompletableFuture<EdgeRpcResponse> result = null;
		switch (request.getMethod()) {
		case EdgeRpcRequest.METHOD:
			result = this.handleEdgeRpcRequest(wsData, user, EdgeRpcRequest.from(request));
		}

		if (result != null) {
			// was able to handle request directly
			return result;
		}

		// forward to generic request handler
		return this.parent.jsonRpcRequestHandler.handleRequest(this.parent.getName(), user, request);
	}

	/**
	 * Gets the authenticated User or throws an Exception if User is not
	 * authenticated.
	 * 
	 * @param wsData  the WebSocket attachment
	 * @param request the JsonrpcRequest
	 * @return the User
	 * @throws OpenemsNamedException if User is not authenticated
	 */
	private BackendUser assertUser(WsData wsData, JsonrpcRequest request) throws OpenemsNamedException {
		Optional<String> userIdOpt = wsData.getUserId();
		if (!userIdOpt.isPresent()) {
			throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED
					.exception("User-ID is empty. Ignoring request [" + request.getMethod() + "]");
		}
		Optional<BackendUser> userOpt = this.parent.metadata.getUser(userIdOpt.get());
		if (!userOpt.isPresent()) {
			throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED.exception("User with ID [" + userIdOpt.get()
					+ "] is unknown. Ignoring request [" + request.getMethod() + "]");
		}
		return userOpt.get();
	}

	/**
	 * Handles an EdgeRpcRequest.
	 * 
	 * @param wsData         the WebSocket attachment
	 * @param backendUser    the authenticated User
	 * @param edgeRpcRequest the EdgeRpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(WsData wsData, BackendUser backendUser,
			EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
		String edgeId = edgeRpcRequest.getEdgeId();
		JsonrpcRequest request = edgeRpcRequest.getPayload();
		User user = backendUser.getAsCommonUser(edgeId);
		user.assertRoleIsAtLeast(EdgeRpcRequest.METHOD, Role.GUEST);

		CompletableFuture<JsonrpcResponseSuccess> resultFuture;
		switch (request.getMethod()) {

		case SubscribeChannelsRequest.METHOD:
			resultFuture = this.handleSubscribeChannelsRequest(wsData, edgeId, user,
					SubscribeChannelsRequest.from(request));
			break;

		case SubscribeSystemLogRequest.METHOD:
			resultFuture = this.handleSubscribeSystemLogRequest(wsData, edgeId, user,
					SubscribeSystemLogRequest.from(request));
			break;

		default:
			// unable to handle; try generic handler
			return null;
		}

		// Wrap reply in EdgeRpcResponse
		CompletableFuture<EdgeRpcResponse> result = new CompletableFuture<EdgeRpcResponse>();
		resultFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				result.complete(new EdgeRpcResponse(edgeRpcRequest.getId(), r));
			} else {
				result.completeExceptionally(
						new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD, request.getMethod()));
			}
		});
		return result;
	}

	/**
	 * Handles a SubscribeChannelsRequest.
	 * 
	 * @param wsData  the WebSocket attachment
	 * @param edgeId  the Edge-ID
	 * @param user    the User - no specific level required
	 * @param request the SubscribeChannelsRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, String edgeId,
			User user, SubscribeChannelsRequest request) throws OpenemsNamedException {
		// activate SubscribedChannelsWorker
		SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
		worker.setEdgeId(edgeId);
		worker.handleSubscribeChannelsRequest(user.getRole(), request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a SubscribeSystemLogRequest.
	 * 
	 * @param wsData  the WebSocket attachment
	 * @param edgeId  the Edge-ID
	 * @param user    the User
	 * @param request the SubscribeSystemLogRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(WsData wsData, String edgeId,
			User user, SubscribeSystemLogRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(SubscribeSystemLogRequest.METHOD, Role.OWNER);
		UUID token = wsData.assertToken();

		// Forward to Edge
		return this.parent.edgeWebsocket.handleSubscribeSystemLogRequest(edgeId, user, token, request);
	}

}
