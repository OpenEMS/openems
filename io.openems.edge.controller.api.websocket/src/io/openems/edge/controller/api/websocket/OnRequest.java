package io.openems.edge.controller.api.websocket;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.AuthenticateWithTokenRequest;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.GetEdgeRequest;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.jsonrpc.request.LogoutRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyPerPeriodRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesExportXlxsRequest;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.SubscribeEdgesRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.AuthenticateResponse;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.GetEdgeResponse;
import io.openems.common.jsonrpc.response.GetEdgesResponse;
import io.openems.common.jsonrpc.response.GetEdgesResponse.EdgeMetadata;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyPerPeriodResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyResponse;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.User;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final ControllerApiWebsocketImpl parent;

	public OnRequest(ControllerApiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		// get websocket attachment
		WsData wsData = ws.getAttachment();

		// Start with authentication requests
		switch (request.getMethod()) {
		case AuthenticateWithTokenRequest.METHOD:
			return this.handleAuthenticateWithTokenRequest(wsData, AuthenticateWithTokenRequest.from(request));

		case AuthenticateWithPasswordRequest.METHOD:
			return this.handleAuthenticateWithPasswordRequest(wsData, AuthenticateWithPasswordRequest.from(request));
		}

		// is user authenticated?
		var user = wsData.assertUserIsAuthenticated(request.getMethod());
		user.assertRoleIsAtLeast(request.getMethod(), Role.GUEST);

		switch (request.getMethod()) {
		case LogoutRequest.METHOD:
			return this.handleLogoutRequest(wsData, user, LogoutRequest.from(request));

		case EdgeRpcRequest.METHOD:
			return this.handleEdgeRpcRequest(wsData, user, EdgeRpcRequest.from(request));

		case GetEdgesRequest.METHOD:
			return this.handleGetEdgesRequest(user, GetEdgesRequest.from(request));

		case GetEdgeRequest.METHOD:
			return this.handleGetEdgeRequest(user, GetEdgeRequest.from(request));

		case SubscribeEdgesRequest.METHOD:
			return this.handleSubscribeEdgesReqeust(user, SubscribeEdgesRequest.from(request));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a {@link LogoutRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param user    the authenticated {@link User}
	 * @param request the {@link LogoutRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleLogoutRequest(WsData wsData, User user,
			LogoutRequest request) throws OpenemsNamedException {
		this.parent.sessionTokens.remove(wsData.getSessionToken(), user);
		wsData.logout();
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link EdgeRpcRequest}.
	 *
	 * @param wsData         the WebSocket attachment
	 * @param edgeRpcRequest the EdgeRpcRequest
	 * @param user           the {@link User}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(WsData wsData, User user,
			EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
		var request = edgeRpcRequest.getPayload();

		CompletableFuture<JsonrpcResponseSuccess> resultFuture;
		switch (request.getMethod()) {

		case SubscribeChannelsRequest.METHOD:
			resultFuture = this.handleSubscribeChannelsRequest(wsData, user, SubscribeChannelsRequest.from(request));
			break;

		case SubscribeSystemLogRequest.METHOD:
			resultFuture = this.handleSubscribeSystemLogRequest(wsData, user, SubscribeSystemLogRequest.from(request));
			break;

		case QueryHistoricTimeseriesDataRequest.METHOD:
			resultFuture = this.handleQueryHistoricDataRequest(user, QueryHistoricTimeseriesDataRequest.from(request));
			break;

		case QueryHistoricTimeseriesEnergyRequest.METHOD:
			resultFuture = this.handleQueryHistoricEnergyRequest(QueryHistoricTimeseriesEnergyRequest.from(request));
			break;

		case QueryHistoricTimeseriesEnergyPerPeriodRequest.METHOD:
			resultFuture = this.handleQueryHistoricEnergyPerPeriodRequest(
					QueryHistoricTimeseriesEnergyPerPeriodRequest.from(request));
			break;

		case QueryHistoricTimeseriesExportXlxsRequest.METHOD:
			resultFuture = this.handleQueryHistoricTimeseriesExportXlxsRequest(user,
					QueryHistoricTimeseriesExportXlxsRequest.from(request));
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

		case GetEdgeConfigRequest.METHOD:
			resultFuture = this.handleGetEdgeConfigRequest(user, GetEdgeConfigRequest.from(request));
			break;

		case SetChannelValueRequest.METHOD:
			resultFuture = this.handleSetChannelValueRequest(user, SetChannelValueRequest.from(request));
			break;

		case ComponentJsonApiRequest.METHOD:
			resultFuture = this.handleComponentJsonApiRequest(user, ComponentJsonApiRequest.from(request));
			break;

		// TODO: to be implemented: UI Logout

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}

		// Wrap reply in EdgeRpcResponse
		var result = new CompletableFuture<EdgeRpcResponse>();
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
	 * Handles a {@link AuthenticateWithTokenRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the {@link AuthenticateWithTokenRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthenticateWithTokenRequest(WsData wsData,
			AuthenticateWithTokenRequest request) throws OpenemsNamedException {
		var token = request.getToken();
		return this.handleAuthentication(wsData, request.getId(),
				Optional.ofNullable(this.parent.sessionTokens.get(token)), token);
	}

	/**
	 * Handles a {@link AuthenticateWithPasswordRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the {@link AuthenticateWithPasswordRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthenticateWithPasswordRequest(WsData wsData,
			AuthenticateWithPasswordRequest request) throws OpenemsNamedException {
		return this.handleAuthentication(wsData, request.getId(),
				this.parent.userService.authenticate(request.password), UUID.randomUUID().toString());
	}

	/**
	 * Common handler for {@link AuthenticateWithTokenRequest} and
	 * {@link AuthenticateWithPasswordRequest}.
	 *
	 * @param wsData    the WebSocket attachment
	 * @param requestId the ID of the original {@link JsonrpcRequest}
	 * @param userOpt   the optional {@link User}
	 * @param token     the existing or new token
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthentication(WsData wsData, UUID requestId,
			Optional<User> userOpt, String token) throws OpenemsNamedException {
		if (userOpt.isPresent()) {
			var user = userOpt.get();
			wsData.setSessionToken(token);
			wsData.setUser(user);
			this.parent.sessionTokens.put(token, user);
			this.parent.logInfo(this.log, "User [" + user.getId() + ":" + user.getName() + "] connected.");

			return CompletableFuture.completedFuture(new AuthenticateResponse(requestId, token, user,
					Utils.getEdgeMetadata(user.getRole()), Language.DEFAULT));
		}
		wsData.unsetUser();
		throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
	}

	/**
	 * Handles a {@link SubscribeChannelsRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param user    the {@link User}
	 * @param request the {@link SubscribeChannelsRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, User user,
			SubscribeChannelsRequest request) throws OpenemsNamedException {
		// Register subscription in WsData
		wsData.handleSubscribeChannelsRequest(request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link QueryHistoricTimeseriesDataRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link QueryHistoricTimeseriesDataRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(User user,
			QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
		var data = this.parent.getTimedata().queryHistoricData(//
				null, /* ignore Edge-ID */
				request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new QueryHistoricTimeseriesDataResponse(request.getId(), data));
	}

	/**
	 * Handles a QueryHistoricEnergyRequest.
	 *
	 * @param request the QueryHistoricEnergyRequest
	 * @return the Future JSPN-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricEnergyRequest(
			QueryHistoricTimeseriesEnergyRequest request) throws OpenemsNamedException {
		Map<ChannelAddress, JsonElement> data = this.parent.getTimedata().queryHistoricEnergy(//
				null, /* ignore Edge-ID */
				request.getFromDate(), request.getToDate(), request.getChannels());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new QueryHistoricTimeseriesEnergyResponse(request.getId(), data));
	}

	/**
	 * Handles a {@link QueryHistoricTimeseriesEnergyPerPeriodRequest}.
	 * 
	 * @param request the {@link QueryHistoricTimeseriesEnergyPerPeriodRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricEnergyPerPeriodRequest(
			QueryHistoricTimeseriesEnergyPerPeriodRequest request) throws OpenemsNamedException {
		var data = this.parent.getTimedata().queryHistoricEnergyPerPeriod(//
				null, /* ignore Edge-ID */
				request.getFromDate(), request.getToDate(), request.getChannels(), request.getResolution());

		return CompletableFuture
				.completedFuture(new QueryHistoricTimeseriesEnergyPerPeriodResponse(request.getId(), data));
	}

	/**
	 * Handles a {@link QueryHistoricTimeseriesExportXlxsRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link QueryHistoricTimeseriesExportXlxsRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricTimeseriesExportXlxsRequest(User user,
			QueryHistoricTimeseriesExportXlxsRequest request) throws OpenemsNamedException {
		return CompletableFuture.completedFuture(
				this.parent.getTimedata().handleQueryHistoricTimeseriesExportXlxsRequest(null /* ignore Edge-ID */,
						request, user.getLanguage()));
	}

	/**
	 * Handles a {@link CreateComponentConfigRequest}.
	 *
	 * @param user                         the {@link User}
	 * @param createComponentConfigRequest the {@link CreateComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(User user,
			CreateComponentConfigRequest createComponentConfigRequest) throws OpenemsNamedException {
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
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(User user,
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
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
	private CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(User user,
			DeleteComponentConfigRequest deleteComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		var request = new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
				deleteComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a {@link GetEdgeConfigRequest}.
	 *
	 * @param user                 the {@link User}
	 * @param getEdgeConfigRequest the {@link GetEdgeConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(User user,
			GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		var request = new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID, getEdgeConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a {@link SetChannelValueRequest}.
	 *
	 * @param user    the User
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
	 * @param user    the User
	 * @param request the {@link ComponentJsonApiRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(User user,
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

		// Wrap reply in new JsonrpcResponseSuccess
		var jsonrpcResponse = new CompletableFuture<JsonrpcResponseSuccess>();
		responseFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				jsonrpcResponse.completeExceptionally(ex);
			} else if (r != null) {
				jsonrpcResponse.complete(new GenericJsonrpcResponseSuccess(request.getId(), r.getResult()));
			} else {
				jsonrpcResponse.completeExceptionally(new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD,
						request.getPayload().getMethod()));
			}
		});

		return jsonrpcResponse;
	}

	/**
	 * Handles a {@link SubscribeSystemLogRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param user    the {@link User}
	 * @param request the {@link SubscribeSystemLogRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(WsData wsData, User user,
			SubscribeSystemLogRequest request) throws OpenemsNamedException {
		var token = wsData.getSessionToken();
		if (token == null) {
			throw OpenemsError.BACKEND_UI_TOKEN_MISSING.exception();
		}
		this.parent.handleSubscribeSystemLogRequest(token, request);
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link GetEdgesRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link GetEdgesRequest}
	 * @return the {@link GetEdgesResponse} Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgesRequest(User user, GetEdgesRequest request) {
		return CompletableFuture.completedFuture(//
				new GetEdgesResponse(request.getId(), Utils.getEdgeMetadata(user.getGlobalRole())));
	}

	/**
	 * Handles a {@link GetEdgeRequest}.
	 * 
	 * @param user    the {@link User}
	 * @param request the {@link GetEdgeRequest}
	 * @return the {@link GetEdgeResponse} Response Future
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeRequest(User user, GetEdgeRequest request) {
		return CompletableFuture.completedFuture(//
				new GetEdgeResponse(request.id, //
						new EdgeMetadata(//
								ControllerApiWebsocket.EDGE_ID, //
								ControllerApiWebsocket.EDGE_COMMENT, //
								ControllerApiWebsocket.EDGE_PRODUCT_TYPE, //
								OpenemsConstants.VERSION, //
								user.getGlobalRole(), //
								true, //
								ZonedDateTime.now(), // lastmessage
								null, // firstSetupProtocol
								ControllerApiWebsocket.SUM_STATE //
						) //
				) //
		);
	}

	/**
	 * Handles a {@link SubscribeEdgesRequest}.
	 * 
	 * @param user    the {@link User}
	 * @param request the {@link SubscribeEdgesRequest}
	 * @return the Response Future
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeEdgesReqeust(User user,
			SubscribeEdgesRequest request) {
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

}
