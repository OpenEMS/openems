package io.openems.edge.controller.api.backend;

import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticatedRpcRequest;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.session.User;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final BackendApi parent;

	public OnRequest(BackendApi parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		// TODO add Check if user Role is sufficient

		switch (request.getMethod()) {

		case AuthenticatedRpcRequest.METHOD:
			return this.handleAuthenticatedRpcRequest(AuthenticatedRpcRequest.from(request));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a AuthenticatedRpcRequest.
	 *
	 * @param getEdgeConfigRequest the AuthenticatedRpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthenticatedRpcRequest(
			AuthenticatedRpcRequest authenticatedRpcRequest) throws OpenemsNamedException {
		User user = authenticatedRpcRequest.getUser();
		JsonrpcRequest request = authenticatedRpcRequest.getPayload();

		switch (request.getMethod()) {

		case GetEdgeConfigRequest.METHOD:
			return this.handleGetEdgeConfigRequest(user, GetEdgeConfigRequest.from(request));

		case UpdateComponentConfigRequest.METHOD:
			return this.handleUpdateComponentConfigRequest(user, UpdateComponentConfigRequest.from(request));

		case ComponentJsonApiRequest.METHOD:
			return this.handleComponentJsonApiRequest(user, ComponentJsonApiRequest.from(request));

		case SubscribeSystemLogRequest.METHOD:
			return this.handleSubscribeSystemLogRequest(user, SubscribeSystemLogRequest.from(request));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}

	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 *
	 * @param user                 the User
	 * @param getEdgeConfigRequest the GetEdgeConfigRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(User user,
			GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
				getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param user                         the User
	 * @param updateComponentConfigRequest the UpdateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(User user,
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a ComponentJsonApiRequest.
	 * 
	 * @param request the ComponentJsonApiRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(User user,
			ComponentJsonApiRequest request) throws OpenemsNamedException {
		// get Component
		String componentId = request.getComponentId();
		OpenemsComponent component = this.parent.componentManager.getComponent(componentId);

		if (component == null) {
			throw new OpenemsException("Unable to find Component [" + componentId + "]");
		}

		if (!(component instanceof JsonApi)) {
			throw new OpenemsException("Component [" + componentId + "] is no JsonApi");
		}

		// call JsonApi
		JsonApi jsonApi = (JsonApi) component;
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = jsonApi.handleJsonrpcRequest(user,
				request.getPayload());

		// handle null response
		if (responseFuture == null) {
			OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getPayload().getMethod());
		}

		// Wrap reply in EdgeRpcResponse
		CompletableFuture<JsonrpcResponseSuccess> edgeRpcResponse = new CompletableFuture<>();
		responseFuture.thenAccept(response -> {
			edgeRpcResponse.complete(new GenericJsonrpcResponseSuccess(request.getId(), response.getResult()));
		});

		return edgeRpcResponse;
	}

	/**
	 * Handles a SubscribeSystemLogRequest.
	 *
	 * @param request the SubscribeSystemLogRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(User user,
			SubscribeSystemLogRequest request) throws OpenemsNamedException {
		this.parent.setSystemLogSubscribed(request.getSubscribe());
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}
}
