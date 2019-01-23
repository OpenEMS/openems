package io.openems.backend.uiwebsocket.impl;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;

import io.openems.backend.metadata.api.User;
import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.GetEdgeConfigResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final UiWebsocketImpl parent;

	public OnRequest(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		WsData wsData = ws.getAttachment();
		User user = this.assertUser(wsData, request);

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
	 * @param user           the authenticated User
	 * @param edgeRpcRequest the EdgeRpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleEdgeRpcRequest(WsData wsData, User user,
			EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
		String edgeId = edgeRpcRequest.getEdgeId();
		JsonrpcRequest request = edgeRpcRequest.getPayload();

		// the Role of this User for this Edge.
		Role role = user.getEdgeRole(edgeId).orElse(Role.GUEST);

		CompletableFuture<JsonrpcResponseSuccess> resultFuture;
		switch (request.getMethod()) {

		case SubscribeChannelsRequest.METHOD:
			resultFuture = this.handleSubscribeChannelsRequest(wsData, edgeId, role,
					SubscribeChannelsRequest.from(request));
			break;

		case QueryHistoricTimeseriesDataRequest.METHOD:
			resultFuture = this.handleQueryHistoricDataRequest(edgeId, role,
					QueryHistoricTimeseriesDataRequest.from(request));
			break;

		case GetEdgeConfigRequest.METHOD:
			resultFuture = this.handleGetEdgeConfigRequest(edgeId, role, GetEdgeConfigRequest.from(request));
			break;

		case UpdateComponentConfigRequest.METHOD:
			resultFuture = this.handleUpdateComponentConfigRequest(edgeId, role,
					UpdateComponentConfigRequest.from(request));
			break;

		default:
			this.parent.logWarn(this.log, "Unhandled EdgeRpcRequest: " + request);
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
	 * Handles a SubscribeChannelsRequest.
	 * 
	 * @param wsData  the WebSocket attachment
	 * @param edgeId  the Edge-ID
	 * @param role    the Role - no specific level required
	 * @param request the SubscribeChannelsRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, String edgeId,
			Role role, SubscribeChannelsRequest request) throws OpenemsNamedException {
		// activate SubscribedChannelsWorker
		SubscribedChannelsWorker worker = wsData.getSubscribedChannelsWorker();
		worker.setEdgeId(edgeId);
		worker.handleSubscribeChannelsRequest(role, request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a QueryHistoricDataRequest.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param role    the Role - no specific level required
	 * @param request the QueryHistoricDataRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(String edgeId, Role role,
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
	 * Handles a GetEdgeConfigRequest.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param role    the Role - no specific level required
	 * @param request the GetEdgeConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(String edgeId, Role role,
			GetEdgeConfigRequest request) throws OpenemsNamedException {
		EdgeConfig config = this.parent.metadata.getEdgeOrError(edgeId).getConfig();

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GetEdgeConfigResponse(request.getId(), config));
	}

	/**
	 * Handles a UpdateComponentConfigRequest.
	 * 
	 * @param edgeId                       the Edge-ID
	 * @param role                         the Role - Installer-level required
	 * @param updateComponentConfigRequest the UpdateComponentConfigRequest
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(String edgeId, Role role,
			UpdateComponentConfigRequest updateComponentConfigRequest) throws OpenemsNamedException {
		if (role.isLessThan(Role.INSTALLER)) {
			throw OpenemsError.COMMON_ROLE_ACCESS_DENIED.exception("UpdateComponentConfig", role);
		}

		// wrap original request inside ComponentJsonApiRequest
		String componentId = OpenemsConstants.COMPONENT_MANAGER_ID;
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, updateComponentConfigRequest);

		return this.parent.edgeWebsocket.send(edgeId, request);
	}

}
