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
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final BackendApiImpl parent;

	public OnRequest(BackendApiImpl parent) {
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
	 * @param authenticatedRpcRequest the AuthenticatedRpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<AuthenticatedRpcResponse> handleAuthenticatedRpcRequest(
			AuthenticatedRpcRequest authenticatedRpcRequest) throws OpenemsNamedException {
		User user = authenticatedRpcRequest.getUser();
		JsonrpcRequest request = authenticatedRpcRequest.getPayload();

		CompletableFuture<? extends JsonrpcResponseSuccess> resultFuture;
		switch (request.getMethod()) {

		case GetEdgeConfigRequest.METHOD:
			resultFuture = this.handleGetEdgeConfigRequest(user, GetEdgeConfigRequest.from(request));
			break;

		case CreateComponentConfigRequest.METHOD:
			resultFuture = this.handleCreateComponentConfigRequest(user, CreateComponentConfigRequest.from(request));
			break;

		case UpdateComponentConfigRequest.METHOD:
			resultFuture = this.handleUpdateComponentConfigRequest(user, UpdateComponentConfigRequest.from(request));
			break;

		case DeleteComponentConfigRequest.METHOD:
			resultFuture = this.handleDeleteComponentConfigRequest(user, DeleteComponentConfigRequest.from(request));
			break;

		case SetChannelValueRequest.METHOD:
			resultFuture = this.handleSetChannelValueRequest(user, SetChannelValueRequest.from(request));
			break;

		case ComponentJsonApiRequest.METHOD:
			resultFuture = this.handleComponentJsonApiRequest(user, ComponentJsonApiRequest.from(request));
			break;

		case SubscribeSystemLogRequest.METHOD:
			resultFuture = this.handleSubscribeSystemLogRequest(user, SubscribeSystemLogRequest.from(request));
			break;

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}

		CompletableFuture<AuthenticatedRpcResponse> result = new CompletableFuture<AuthenticatedRpcResponse>();
		resultFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				result.complete(new AuthenticatedRpcResponse(authenticatedRpcRequest.getId(), r));
			} else {
				result.completeExceptionally(
						new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD, request.getMethod()));
			}
		});
		return result;
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 *
	 * @param user                 the User
	 * @param getEdgeConfigRequest the GetEdgeConfigRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleGetEdgeConfigRequest(User user,
			GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
				getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a CreateComponentConfigRequest.
	 * 
	 * @param user                         the User
	 * @param createComponentConfigRequest the CreateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleCreateComponentConfigRequest(User user,
			CreateComponentConfigRequest createComponentConfigRequest) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.INSTALLER);

		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, createComponentConfigRequest);

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
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleUpdateComponentConfigRequest(User user,
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.OWNER);

		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a DeleteComponentConfigRequest.
	 * 
	 * @param user                         the User
	 * @param deleteComponentConfigRequest the DeleteComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleDeleteComponentConfigRequest(User user,
			DeleteComponentConfigRequest deleteComponentConfigRequest) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.INSTALLER);

		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, deleteComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a SetChannelValueRequest.
	 * 
	 * @param user    the User
	 * @param request the SetChannelValueRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(User user,
			SetChannelValueRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(SetChannelValueRequest.METHOD, Role.ADMIN);

		return this.parent.apiWorker.handleSetChannelValueRequest(this.parent.componentManager, user, request);
	}

	/**
	 * Handles a ComponentJsonApiRequest.
	 * 
	 * @param user    the {@link User}
	 * @param request the ComponentJsonApiRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleComponentJsonApiRequest(User user,
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
		CompletableFuture<? extends JsonrpcResponseSuccess> responseFuture = jsonApi.handleJsonrpcRequest(user,
				request.getPayload());

		// handle null response
		if (responseFuture == null) {
			OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getPayload().getMethod());
		}

		// Wrap reply in EdgeRpcResponse
		CompletableFuture<GenericJsonrpcResponseSuccess> edgeRpcResponse = new CompletableFuture<>();
		responseFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				edgeRpcResponse.completeExceptionally(ex);
			} else if (r != null) {
				edgeRpcResponse.complete(new GenericJsonrpcResponseSuccess(request.getId(), r.getResult()));
			} else {
				edgeRpcResponse.completeExceptionally(new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD,
						request.getPayload().getMethod()));
			}
		});

		return edgeRpcResponse;
	}

	/**
	 * Handles a SubscribeSystemLogRequest.
	 *
	 * @param user    the {@link User}
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
