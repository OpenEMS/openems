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
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.AuthenticatedRpcResponse;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final BackendApi parent;

	public OnRequest(BackendApi parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
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
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<AuthenticatedRpcResponse> handleAuthenticatedRpcRequest(
			AuthenticatedRpcRequest authenticatedRpcRequest) throws OpenemsNamedException {
		JsonrpcRequest request = authenticatedRpcRequest.getPayload();

		CompletableFuture<? extends JsonrpcResponseSuccess> resultFuture;
		switch (request.getMethod()) {

		case GetEdgeConfigRequest.METHOD:
			resultFuture = this.handleGetEdgeConfigRequest(GetEdgeConfigRequest.from(request));
			break;

		case CreateComponentConfigRequest.METHOD:
			resultFuture = this.handleCreateComponentConfigRequest(CreateComponentConfigRequest.from(request));
			break;

		case UpdateComponentConfigRequest.METHOD:
			resultFuture = this.handleUpdateComponentConfigRequest(UpdateComponentConfigRequest.from(request));
			break;

		case DeleteComponentConfigRequest.METHOD:
			resultFuture = this.handleDeleteComponentConfigRequest(DeleteComponentConfigRequest.from(request));
			break;

		case SetChannelValueRequest.METHOD:
			resultFuture = this.handleSetChannelValueRequest(SetChannelValueRequest.from(request));
			break;

		case ComponentJsonApiRequest.METHOD:
			resultFuture = this.handleComponentJsonApiRequest(ComponentJsonApiRequest.from(request));
			break;

		case SubscribeSystemLogRequest.METHOD:
			resultFuture = this.handleSubscribeSystemLogRequest(SubscribeSystemLogRequest.from(request));
			break;

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}

		CompletableFuture<AuthenticatedRpcResponse> result = new CompletableFuture<AuthenticatedRpcResponse>();
		resultFuture.thenAccept(r -> {
			result.complete(new AuthenticatedRpcResponse(authenticatedRpcRequest.getId(), r));
		});
		return result;
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 *
	 * @param getEdgeConfigRequest the GetEdgeConfigRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleGetEdgeConfigRequest(GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
				getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a CreateComponentConfigRequest.
	 *
	 * @param createComponentConfigRequest the CreateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleCreateComponentConfigRequest(CreateComponentConfigRequest createComponentConfigRequest) throws OpenemsNamedException {
		// TODO authorization
		// user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.INSTALLER);

		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, createComponentConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 *
	 * @param updateComponentConfigRequest the UpdateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleUpdateComponentConfigRequest(UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		// TODO authorization
		// user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.OWNER);

		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a DeleteComponentConfigRequest.
	 *
	 * @param deleteComponentConfigRequest the DeleteComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleDeleteComponentConfigRequest(DeleteComponentConfigRequest deleteComponentConfigRequest) throws OpenemsNamedException {
		// TODO authorization
		// user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.INSTALLER);

		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, deleteComponentConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a SetChannelValueRequest.
	 *
	 * @param request the SetChannelValueRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(SetChannelValueRequest request) throws OpenemsNamedException {
		// TODO authorization
		// user.assertRoleIsAtLeast(SetChannelValueRequest.METHOD, Role.ADMIN);

		return this.parent.apiWorker.handleSetChannelValueRequest(this.parent.componentManager, request);
	}

	/**
	 * Handles a ComponentJsonApiRequest.
	 *
	 *
	 * @param request the ComponentJsonApiRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
    private CompletableFuture<GenericJsonrpcResponseSuccess> handleComponentJsonApiRequest(ComponentJsonApiRequest request) throws OpenemsNamedException {
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
		CompletableFuture<JsonrpcResponseSuccess> responseFuture = jsonApi.handleJsonrpcRequest(
			request.getPayload());

		// handle null response
		if (responseFuture == null) {
			OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getPayload().getMethod());
		}

		// Wrap reply in EdgeRpcResponse
		CompletableFuture<GenericJsonrpcResponseSuccess> edgeRpcResponse = new CompletableFuture<>();
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
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(SubscribeSystemLogRequest request) throws OpenemsNamedException {
		this.parent.setSystemLogSubscribed(request.getSubscribe());
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}
}
