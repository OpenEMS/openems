package io.openems.edge.controller.api.websocket;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.AuthenticateWithPasswordResponse;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.websocket.SubscribedChannelsWorker;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final WebsocketApi parent;

	public OnRequest(WebsocketApi parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		// get websocket attachment
		WsData wsData = ws.getAttachment();

		// special handling for 'authenticate' request
		if (request.getMethod().equals(AuthenticateWithPasswordRequest.METHOD)) {
			return this.handleAuthenticateWithPasswordRequest(wsData, AuthenticateWithPasswordRequest.from(request));
		}

		// is user authenticated?
		if (!wsData.isUserAuthenticated()) {
			throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED.exception(
					"Session [" + wsData.getSessionToken() + "]. Ignoring request [" + request.getMethod() + "]");
		}

		// TODO add Check if user Role is sufficient

		switch (request.getMethod()) {

		case EdgeRpcRequest.METHOD:
			return this.handleEdgeRpcRequest(wsData, EdgeRpcRequest.from(request));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles an EdgeRpcRequest.
	 * 
	 * @param wsData         the WebSocket attachment
	 * @param edgeRpcRequest the EdgeRpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleEdgeRpcRequest(WsData wsData, EdgeRpcRequest edgeRpcRequest)
			throws OpenemsNamedException {
		JsonrpcRequest request = edgeRpcRequest.getPayload();

		CompletableFuture<JsonrpcResponseSuccess> resultFuture;
		switch (request.getMethod()) {

		case SubscribeChannelsRequest.METHOD:
			resultFuture = this.handleSubscribeChannelsRequest(wsData, SubscribeChannelsRequest.from(request));
			break;

		case SubscribeSystemLogRequest.METHOD:
			resultFuture = this.handleSubscribeSystemLogRequest(wsData, SubscribeSystemLogRequest.from(request));
			break;
			
		case QueryHistoricTimeseriesDataRequest.METHOD:
			resultFuture = this.handleQueryHistoricDataRequest(QueryHistoricTimeseriesDataRequest.from(request));
			break;

		case UpdateComponentConfigRequest.METHOD:
			resultFuture = this.handleUpdateComponentConfigRequest(UpdateComponentConfigRequest.from(request));
			break;

		case GetEdgeConfigRequest.METHOD:
			resultFuture = this.handleGetEdgeConfigRequest(GetEdgeConfigRequest.from(request));
			break;

		case ComponentJsonApiRequest.METHOD:
			resultFuture = this.handleComponentJsonApiRequest(ComponentJsonApiRequest.from(request));
			break;

		// TODO: to be implemented: UI Logout

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}

		// Get Response
		JsonrpcResponseSuccess result;
		try {
			result = resultFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			if (e.getCause() instanceof OpenemsNamedException) {
				throw (OpenemsNamedException) e.getCause();
			} else {
				throw OpenemsError.GENERIC.exception(e.getMessage());
			}
		}

		// Wrap reply in EdgeRpcResponse
		return CompletableFuture.completedFuture(new EdgeRpcResponse(edgeRpcRequest.getId(), result));
	}

	/**
	 * Handles a AuthenticateWithPasswordRequest.
	 * 
	 * @param wsData  the WebSocket attachment
	 * @param request the AuthenticateWithPasswordRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthenticateWithPasswordRequest(WsData wsData,
			AuthenticateWithPasswordRequest request) throws OpenemsNamedException {
		Optional<User> userOpt = this.parent.userService.authenticate(request.getPassword());
		if (!userOpt.isPresent()) {
			wsData.unsetUser();
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}

		// authentication successful
		User user = userOpt.get();
		wsData.setUser(user);
		this.parent.sessionTokens.put(wsData.getSessionToken(), user);
		// TODO unset on logout!
		return CompletableFuture.completedFuture(new AuthenticateWithPasswordResponse(request.getId(),
				wsData.getSessionToken(), Utils.getEdgeMetadata(user.getRole())));
	}

	/**
	 * Handles a SubscribeChannelsRequest.
	 * 
	 * @param wsData  the WebSocket attachment
	 * @param request the SubscribeChannelsRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData,
			SubscribeChannelsRequest request) throws OpenemsNamedException {
		// activate SubscribedChannelsWorker
		SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
		worker.handleSubscribeChannelsRequest(Role.GUEST /* TODO: get Role as parameter */, request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a QueryHistoricDataRequest.
	 * 
	 * @param request the QueryHistoricDataRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(
			QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
		TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> data;
		data = this.parent.timedata.queryHistoricData(//
				null, /* igore Edge-ID */
				request.getFromDate(), //
				request.getToDate(), //
				request.getChannels());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new QueryHistoricTimeseriesDataResponse(request.getId(), data));
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param updateComponentConfigRequest the UpdateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 * 
	 * @param getEdgeConfigRequest the GetEdgeConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(
			GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
				getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(request);
	}

	/**
	 * Handles a ComponentJsonApiRequest.
	 * 
	 * @param request the ComponentJsonApiRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(ComponentJsonApiRequest request)
			throws OpenemsNamedException {
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
		JsonrpcResponseSuccess response = jsonApi.handleJsonrpcRequest(request.getPayload());

		// return response
		return CompletableFuture
				.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), response.getResult()));
	}

	/**
	 * Handles a SubscribeSystemLogRequest.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the SubscribeSystemLogRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(WsData wsData,
			SubscribeSystemLogRequest request) throws OpenemsNamedException {
		UUID token = wsData.getSessionToken();
		if (token == null) {
			throw OpenemsError.BACKEND_UI_TOKEN_MISSING.exception();
		}
		this.parent.handleSubscribeSystemLogRequest(token, request);
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

}
