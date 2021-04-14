package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.response.AuthenticateWithPasswordResponse;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.session.Role;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final UiWebsocketImpl parent;

	public OnRequest(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		WsData wsData = ws.getAttachment();

		CompletableFuture<? extends JsonrpcResponseSuccess> result = null;
		if (request.getMethod().equals(AuthenticateWithPasswordRequest.METHOD)) {
			// trying to authenticate
			return this.handleAuthenticateWithPasswordRequest(wsData, AuthenticateWithPasswordRequest.from(request));
		}

		// should be authenticated
		User user = this.assertUser(wsData, request);

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
	 * Handles an {@link AuthenticateWithPasswordRequest}.
	 * 
	 * @param wsData  the WebSocket attachment
	 * @param request the {@link AuthenticateWithPasswordRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthenticateWithPasswordRequest(WsData wsData,
			AuthenticateWithPasswordRequest request) throws OpenemsNamedException {
		final User user;
		if (request.getUsername().isPresent()) {
			user = this.parent.metadata.authenticate(request.getUsername().get(), request.getPassword());
		} else {
			user = this.parent.metadata.authenticate(request.getPassword());
		}
		wsData.setUserId(user.getId());
		return CompletableFuture.completedFuture(new AuthenticateWithPasswordResponse(request.getId(),
				user.getSessionId(), user.getEdgeMetadatas(this.parent.metadata)));
	}

	/**
	 * Gets the authenticated User or throws an Exception if User is not
	 * authenticated.
	 * 
	 * @param wsData  the WebSocket attachment
	 * @param request the JsonrpcRequest
	 * @return the {@link User}
	 * @throws OpenemsNamedException if User is not authenticated
	 */
	private User assertUser(WsData wsData, JsonrpcRequest request) throws OpenemsNamedException {
		Optional<String> userIdOpt = wsData.getUserId();
		if (!userIdOpt.isPresent()) {
			throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED
					.exception("User-ID is empty. Ignoring request [" + request.getMethod() + "]");
		}
		Optional<User> userOpt = this.parent.metadata.getUser(userIdOpt.get());
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
	 * @param user           the authenticated {@link User}
	 * @param edgeRpcRequest the {@link EdgeRpcRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(WsData wsData, User user,
			EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
		String edgeId = edgeRpcRequest.getEdgeId();
		JsonrpcRequest request = edgeRpcRequest.getPayload();
		user.assertEdgeRoleIsAtLeast(EdgeRpcRequest.METHOD, edgeId, Role.GUEST);

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
	 * @param user    the {@link User} - no specific level required
	 * @param request the SubscribeChannelsRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, String edgeId,
			User user, SubscribeChannelsRequest request) throws OpenemsNamedException {
		// activate SubscribedChannelsWorker
		SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker(edgeId);
		worker.handleSubscribeChannelsRequest(user.getRole(edgeId).orElse(Role.GUEST), request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link SubscribeSystemLogRequest}.
	 * 
	 * @param wsData  the WebSocket attachment
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User}
	 * @param request the {@link SubscribeSystemLogRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(WsData wsData, String edgeId,
			User user, SubscribeSystemLogRequest request) throws OpenemsNamedException {
		user.assertEdgeRoleIsAtLeast(SubscribeSystemLogRequest.METHOD, edgeId, Role.OWNER);
		UUID token = wsData.assertToken();

		// Forward to Edge
		return this.parent.edgeWebsocket.handleSubscribeSystemLogRequest(edgeId, user, token, request);
	}

}
