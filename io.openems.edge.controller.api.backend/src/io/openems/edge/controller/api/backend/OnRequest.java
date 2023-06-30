package io.openems.edge.controller.api.backend;

import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.NotImplementedException;
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
import io.openems.common.jsonrpc.request.UpdateUserLanguageRequest;
import io.openems.common.jsonrpc.response.AuthenticatedRpcResponse;
import io.openems.common.session.Role;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final ControllerApiBackendImpl parent;

	public OnRequest(ControllerApiBackendImpl parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		switch (request.getMethod()) {

		case AuthenticatedRpcRequest.METHOD:
			return this.handleAuthenticatedRpcRequest(AuthenticatedRpcRequest.<User>from(request, User::from));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a {@link AuthenticatedRpcRequest}.
	 *
	 * @param authenticatedRpcRequest the {@link AuthenticatedRpcRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<AuthenticatedRpcResponse> handleAuthenticatedRpcRequest(
			AuthenticatedRpcRequest<User> authenticatedRpcRequest) throws OpenemsNamedException {
		var user = authenticatedRpcRequest.getUser();
		var request = authenticatedRpcRequest.getPayload();

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

		case UpdateUserLanguageRequest.METHOD:
			resultFuture = this.handleUpdateUserLanguageRequest(user, UpdateUserLanguageRequest.from(request));
			break;

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}

		var result = new CompletableFuture<AuthenticatedRpcResponse>();
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
	 * Handles a {@link GetEdgeConfigRequest}.
	 *
	 * @param user                 the {@link User}
	 * @param getEdgeConfigRequest the {@link GetEdgeConfigRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleGetEdgeConfigRequest(User user,
			GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		var request = new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID, getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a {@link CreateComponentConfigRequest}.
	 *
	 * @param user                         the {@link User}
	 * @param createComponentConfigRequest the {@link CreateComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleCreateComponentConfigRequest(User user,
			CreateComponentConfigRequest createComponentConfigRequest) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.INSTALLER);

		// wrap original request inside ComponentJsonApiRequest
		var request = new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
				createComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a {@link UpdateComponentConfigRequest}.
	 *
	 * @param user                         the {@link User}
	 * @param updateComponentConfigRequest the {@link UpdateComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleUpdateComponentConfigRequest(User user,
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.OWNER);

		// wrap original request inside ComponentJsonApiRequest
		var request = new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
				updateComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a {@link DeleteComponentConfigRequest}.
	 *
	 * @param user                         the {@link User}
	 * @param deleteComponentConfigRequest the {@link DeleteComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleDeleteComponentConfigRequest(User user,
			DeleteComponentConfigRequest deleteComponentConfigRequest) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.INSTALLER);

		// wrap original request inside ComponentJsonApiRequest
		var request = new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
				deleteComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a {@link SetChannelValueRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link SetChannelValueRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(User user,
			SetChannelValueRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(SetChannelValueRequest.METHOD, Role.ADMIN);

		return this.parent.apiWorker.handleSetChannelValueRequest(this.parent.componentManager, user, request);
	}

	/**
	 * Handles a {@link ComponentJsonApiRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link ComponentJsonApiRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleComponentJsonApiRequest(User user,
			ComponentJsonApiRequest request) throws OpenemsNamedException {
		// get Component
		var componentId = request.getComponentId();
		var component = this.parent.componentManager.getComponent(componentId);

		if (component == null) {
			throw new OpenemsException("Unable to find Component [" + componentId + "]");
		}

		if (!(component instanceof JsonApi)) {
			throw new OpenemsException("Component [" + componentId + "] is no JsonApi");
		}

		// call JsonApi
		var jsonApi = (JsonApi) component;
		CompletableFuture<? extends JsonrpcResponseSuccess> responseFuture = jsonApi.handleJsonrpcRequest(user,
				request.getPayload());

		// handle null response
		if (responseFuture == null) {
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getPayload().getMethod());
		}

		// Wrap reply in EdgeRpcResponse
		var edgeRpcResponse = new CompletableFuture<GenericJsonrpcResponseSuccess>();
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
	 * Handles a {@link SubscribeSystemLogRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link SubscribeSystemLogRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(User user,
			SubscribeSystemLogRequest request) throws OpenemsNamedException {
		this.parent.setSystemLogSubscribed(request.isSubscribe());
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link UpdateUserLanguageRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link UpdateUserLanguageRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateUserLanguageRequest(User user,
			UpdateUserLanguageRequest request) throws OpenemsNamedException {
		throw new NotImplementedException("Edge backend api update user language not implemented");
	}

}
