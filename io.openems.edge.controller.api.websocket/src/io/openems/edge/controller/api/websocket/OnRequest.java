package io.openems.edge.controller.api.websocket;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyPerPeriodRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesExportXlxsRequest;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateSoftwareRequest;
import io.openems.common.jsonrpc.response.AuthenticateWithPasswordResponse;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyPerPeriodResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyResponse;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.websocket.SubscribedChannelsWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.user.EdgeUser;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final WebsocketApi parent;

	public OnRequest(WebsocketApi parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		// get websocket attachment
		WsData wsData = ws.getAttachment();

		// special handling for 'authenticate' request
		if (request.getMethod().equals(AuthenticateWithPasswordRequest.METHOD)) {
			return this.handleAuthenticateWithPasswordRequest(wsData, AuthenticateWithPasswordRequest.from(request));
		}

		// is user authenticated?
		EdgeUser user = wsData.assertUserIsAuthenticated(request.getMethod());
		user.assertRoleIsAtLeast(request.getMethod(), Role.GUEST);

		switch (request.getMethod()) {

		case EdgeRpcRequest.METHOD:
			return this.handleEdgeRpcRequest(wsData, user, EdgeRpcRequest.from(request));

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
	 * @param user           the User
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(WsData wsData, EdgeUser user,
			EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
		JsonrpcRequest request = edgeRpcRequest.getPayload();

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

		case UpdateSoftwareRequest.METHOD:
			resultFuture = this.handleUpdateSoftwareRequest(this.parent, wsData, UpdateSoftwareRequest.from(request));
			break;

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
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

	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateSoftwareRequest(WebsocketApi parent, WsData wsData,
			UpdateSoftwareRequest request) throws OpenemsNamedException {
		JsonObject result = new JsonObject();
		int success = 0;
		int error = 0;

		WebSocket ws = wsData.getWebsocket();

		OpenemsComponent kacoUpdateComponent = this.parent.componentManager.getComponent("_kacoUpdate");

		Channel<Integer> hasUpdateChannel = kacoUpdateComponent.channel("HasUpdate");
		int hasUpdate = hasUpdateChannel.value().get().intValue();

		try {

			if (hasUpdate != 2) {

				KacoUpdateHandler.updateUI(parent, ws);
				success++;
				hasUpdate--;
			}

		} catch (IllegalArgumentException | IOException e) {
			error++;
			e.printStackTrace();
			this.log.error(e.getMessage(), e);
			try {
				KacoUpdateHandler.uiRestore(parent, ws);
			} catch (IOException e1) {

				this.log.error(e1.getMessage(), e1);
			}
		}
		try {

			if (hasUpdate > 1) {

				KacoUpdateHandler.updateEdge(ws, parent);
				success += 2;
				hasUpdate -= 2;

			}
		} catch (IllegalArgumentException | IOException e) {
			error += 2;
			KacoUpdateHandler.restoreEdge();
			this.log.error(e.getMessage(), e);
		}
		hasUpdateChannel.setNextValue(hasUpdate);
		result.addProperty("Success", success);
		result.addProperty("Error", error);
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), result));
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
		Optional<EdgeUser> userOpt = this.parent.userService.authenticate(request.getPassword());
		if (!userOpt.isPresent()) {
			wsData.unsetUser();
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}

		// authentication successful
		EdgeUser user = userOpt.get();
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
	 * @param user    the User
	 * @param request the SubscribeChannelsRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, EdgeUser user,
			SubscribeChannelsRequest request) throws OpenemsNamedException {
		// activate SubscribedChannelsWorker
		SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
		worker.handleSubscribeChannelsRequest(user.getRole(), request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a QueryHistoricDataRequest.
	 * 
	 * @param user    the User
	 * @param request the QueryHistoricDataRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(EdgeUser user,
			QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data = this.parent.getTimedata()
				.queryHistoricData(//
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
	 * Handles a QueryHistoricTimeseriesExportXlxsRequest.
	 * 
	 * @param user    the User
	 * @param request the QueryHistoricTimeseriesExportXlxsRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricTimeseriesExportXlxsRequest(EdgeUser user,
			QueryHistoricTimeseriesExportXlxsRequest request) throws OpenemsNamedException {
		return CompletableFuture.completedFuture(this.parent.getTimedata()
				.handleQueryHistoricTimeseriesExportXlxsRequest(null /* ignore Edge-ID */, request));
	}

	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricEnergyPerPeriodRequest(
			QueryHistoricTimeseriesEnergyPerPeriodRequest request) throws OpenemsNamedException {

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data = this.parent.getTimedata()
				.queryHistoricEnergyPerPeriod(//
						null, request.getFromDate(), request.getToDate(), request.getChannels(),
						request.getResolution());

		return CompletableFuture
				.completedFuture(new QueryHistoricTimeseriesEnergyPerPeriodResponse(request.getId(), data));
	}

	/**
	 * Handles a CreateComponentConfigRequest.
	 * 
	 * @param user                         the User
	 * @param createComponentConfigRequest the CreateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(EdgeUser user,
			CreateComponentConfigRequest createComponentConfigRequest) throws OpenemsNamedException {
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
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(EdgeUser user,
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
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
	private CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(EdgeUser user,
			DeleteComponentConfigRequest deleteComponentConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, deleteComponentConfigRequest);

		return this.handleComponentJsonApiRequest(user, request);
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 * 
	 * @param user                 the User
	 * @param getEdgeConfigRequest the GetEdgeConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(EdgeUser user,
			GetEdgeConfigRequest getEdgeConfigRequest) throws OpenemsNamedException {
		// wrap original request inside ComponentJsonApiRequest
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(OpenemsConstants.COMPONENT_MANAGER_ID,
				getEdgeConfigRequest);

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
	private CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(EdgeUser user,
			SetChannelValueRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(SetChannelValueRequest.METHOD, Role.ADMIN);

		return this.parent.apiWorker.handleSetChannelValueRequest(this.parent.componentManager, user, request);
	}

	/**
	 * Handles a ComponentJsonApiRequest.
	 * 
	 * @param user    the User
	 * @param request the ComponentJsonApiRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(EdgeUser user,
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

		// Wrap reply in new JsonrpcResponseSuccess
		CompletableFuture<JsonrpcResponseSuccess> jsonrpcResponse = new CompletableFuture<>();
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
	 * Handles a SubscribeSystemLogRequest.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param user    the User
	 * @param request the SubscribeSystemLogRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(WsData wsData, EdgeUser user,
			SubscribeSystemLogRequest request) throws OpenemsNamedException {
		UUID token = wsData.getSessionToken();
		if (token == null) {
			throw OpenemsError.BACKEND_UI_TOKEN_MISSING.exception();
		}
		this.parent.handleSubscribeSystemLogRequest(token, request);
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

}
