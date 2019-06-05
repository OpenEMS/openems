package io.openems.backend.uiwebsocket.impl;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyRequest;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.GetEdgeConfigResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyResponse;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final UiWebsocketImpl parent;

	public OnRequest(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		WsData wsData = ws.getAttachment();
		BackendUser user = this.assertUser(wsData, request);

		switch (request.getMethod()) {
		case EdgeRpcRequest.METHOD:
			return this.handleEdgeRpcRequest(wsData, user, EdgeRpcRequest.from(request));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
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

		case QueryHistoricTimeseriesDataRequest.METHOD:
			resultFuture = this.handleQueryHistoricDataRequest(edgeId, user,
					QueryHistoricTimeseriesDataRequest.from(request));
			break;

		case QueryHistoricTimeseriesEnergyRequest.METHOD:
			resultFuture = this.handleQueryHistoricEnergyRequest(edgeId, user,
					QueryHistoricTimeseriesEnergyRequest.from(request));
			break;

		case GetEdgeConfigRequest.METHOD:
			resultFuture = this.handleGetEdgeConfigRequest(edgeId, user, GetEdgeConfigRequest.from(request));
			break;

		case CreateComponentConfigRequest.METHOD:
			resultFuture = this.handleCreateComponentConfigRequest(edgeId, user,
					CreateComponentConfigRequest.from(request));
			break;

		case UpdateComponentConfigRequest.METHOD:
			resultFuture = this.handleUpdateComponentConfigRequest(edgeId, user,
					UpdateComponentConfigRequest.from(request));
			break;

		case DeleteComponentConfigRequest.METHOD:
			resultFuture = this.handleDeleteComponentConfigRequest(edgeId, user,
					DeleteComponentConfigRequest.from(request));
			break;

		case SetChannelValueRequest.METHOD:
			resultFuture = this.handleSetChannelValueRequest(edgeId, user, SetChannelValueRequest.from(request));
			break;

		case ComponentJsonApiRequest.METHOD:
			resultFuture = this.handleComponentJsonApiRequest(edgeId, user, ComponentJsonApiRequest.from(request));
			break;

		default:
			this.parent.logWarn(this.log, "Unhandled EdgeRpcRequest: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}

		// Wrap reply in EdgeRpcResponse
		CompletableFuture<EdgeRpcResponse> result = new CompletableFuture<EdgeRpcResponse>();
		resultFuture.thenAccept(r -> {
			result.complete(new EdgeRpcResponse(edgeRpcRequest.getId(), r));
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

	/**
	 * Handles a QueryHistoricDataRequest.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param user    the User - no specific level required
	 * @param request the QueryHistoricDataRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(String edgeId, User user,
			QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
		TreeBasedTable<ZonedDateTime, ChannelAddress, JsonElement> data;
		data = this.parent.timeData.queryHistoricData(//
				edgeId, //
				request.getFromDate(), //
				request.getToDate(), //
				request.getChannels());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new QueryHistoricTimeseriesDataResponse(request.getId(), data));
	}

	/**
	 * Handles a QueryHistoricEnergyequest.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param user    the User - no specific level required
	 * @param request the QueryHistoricEnergyRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricEnergyRequest(String edgeId, User user,
			QueryHistoricTimeseriesEnergyRequest request) throws OpenemsNamedException {
		Map<ChannelAddress, JsonElement> data;
		data = this.parent.timeData.queryHistoricEnergy(//
				edgeId, /* ignore Edge-ID */
				request.getFromDate(), request.getToDate(), request.getChannels());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new QueryHistoricTimeseriesEnergyResponse(request.getId(), data));
	}

	/**
	 * Handles a GetEdgeConfigRequest.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param user    the User - no specific level required
	 * @param request the GetEdgeConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(String edgeId, User user,
			GetEdgeConfigRequest request) throws OpenemsNamedException {
		EdgeConfig config = this.parent.metadata.getEdgeOrError(edgeId).getConfig();

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GetEdgeConfigResponse(request.getId(), config));
	}

	/**
	 * Handles a CreateComponentConfigRequest.
	 * 
	 * @param edgeId                       the Edge-ID
	 * @param user                         the User - Installer-level required
	 * @param createComponentConfigRequest the CreateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(String edgeId, User user,
			CreateComponentConfigRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(CreateComponentConfigRequest.METHOD, Role.INSTALLER);

		return this.parent.edgeWebsocket.send(edgeId, user, request);
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param edgeId                       the Edge-ID
	 * @param user                         the User - Installer-level required
	 * @param updateComponentConfigRequest the UpdateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(String edgeId, User user,
			UpdateComponentConfigRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(UpdateComponentConfigRequest.METHOD, Role.OWNER);

		return this.parent.edgeWebsocket.send(edgeId, user, request);
	}

	/**
	 * Handles a DeleteComponentConfigRequest.
	 * 
	 * @param edgeId                       the Edge-ID
	 * @param user                         the User - Installer-level required
	 * @param updateComponentConfigRequest the DeleteComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(String edgeId, User user,
			DeleteComponentConfigRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, Role.INSTALLER);

		return this.parent.edgeWebsocket.send(edgeId, user, request);
	}

	/**
	 * Handles a SetChannelValueRequest.
	 * 
	 * @param user    the User
	 * @param request the SetChannelValueRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(String edgeId, User user,
			SetChannelValueRequest request) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(SetChannelValueRequest.METHOD, Role.ADMIN);

		return this.parent.edgeWebsocket.send(edgeId, user, request);
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param edgeId                  the Edge-ID
	 * @param user                    the User - Installer-level required
	 * @param componentJsonApiRequest the ComponentJsonApiRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(String edgeId, User user,
			ComponentJsonApiRequest componentJsonApiRequest) throws OpenemsNamedException {
		user.assertRoleIsAtLeast(ComponentJsonApiRequest.METHOD, Role.GUEST);

		return this.parent.edgeWebsocket.send(edgeId, user, componentJsonApiRequest);
	}

}
