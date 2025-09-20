package io.openems.backend.b2bwebsocket;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;

import io.openems.backend.b2bwebsocket.jsonrpc.request.SubscribeEdgesChannelsRequest;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.session.Role;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Backend2BackendWebsocket parent;

	public OnRequest(Backend2BackendWebsocket parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> apply(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		WsData wsData = ws.getAttachment();
		var user = wsData.getUserWithTimeout(5, TimeUnit.SECONDS);

		switch (request.getMethod()) {

		case SubscribeEdgesChannelsRequest.METHOD:
			return this.handleSubscribeEdgesChannelsRequest(wsData, user, request.getId(),
					SubscribeEdgesChannelsRequest.from(request));
		}

		// Forward to generic handler
		return this.parent.jsonRpcRequestHandler.handleRequest(this.parent.getName(), user, request);
	}

	/**
	 * Handles a {@link SubscribeEdgesChannelsRequest}.
	 *
	 * @param wsData    the WebSocket attachment
	 * @param user      the {@link User}
	 * @param messageId the JSON-RPC Message-ID
	 * @param request   the {@link SubscribeEdgesChannelsRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleSubscribeEdgesChannelsRequest(WsData wsData,
			User user, UUID messageId, SubscribeEdgesChannelsRequest request) throws OpenemsNamedException {
		for (var edgeId : request.getEdgeIds()) {
			if (user.getRole(edgeId).isEmpty()) {
				this.parent.metadata.getEdgeMetadataForUser(user, edgeId);
			}

			// assure read permissions of this User for this Edge.
			user.assertEdgeRoleIsAtLeast(SubscribeEdgesChannelsRequest.METHOD, edgeId, Role.GUEST);
		}

		// activate SubscribedChannelsWorker
		var worker = wsData.getSubscribedChannelsWorker();
		worker.handleSubscribeEdgesChannelsRequest(request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

}
