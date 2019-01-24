package io.openems.backend.b2bwebsocket;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.GetChannelsValuesRequest;
import io.openems.common.jsonrpc.request.GetStatusOfEdgesRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.jsonrpc.response.GetChannelsValuesResponse;
import io.openems.common.jsonrpc.response.GetStatusOfEdgesResponse;
import io.openems.common.jsonrpc.response.GetStatusOfEdgesResponse.EdgeInfo;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final B2bWebsocket parent;

	public OnRequest(B2bWebsocket parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsException, OpenemsNamedException {
		WsData wsData = ws.getAttachment();
		User user = wsData.assertUser();

		switch (request.getMethod()) {

		case GetStatusOfEdgesRequest.METHOD:
			return this.handleGetStatusOfEdgesRequest(user, request.getId(), GetStatusOfEdgesRequest.from(request));

		case GetChannelsValuesRequest.METHOD:
			return this.handleGetChannelsValuesRequest(user, request.getId(), GetChannelsValuesRequest.from(request));

		case SetGridConnScheduleRequest.METHOD:
			return this.handleSetGridConnScheduleRequest(user, request.getId(),
					SetGridConnScheduleRequest.from(request));

		default:
			this.parent.logWarn(this.log, "Unhandled Request: " + request);
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a GetStatusOfEdgesRequest.
	 * 
	 * @param user      the User
	 * @param messageId the JSON-RPC Message-ID
	 * @param request   the GetStatusOfEdgesRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetStatusOfEdgesRequest(User user, UUID messageId,
			GetStatusOfEdgesRequest request) throws OpenemsNamedException {
		Map<String, EdgeInfo> result = new HashMap<>();
		for (Entry<String, Role> entry : user.getEdgeRoles().entrySet()) {
			String edgeId = entry.getKey();
			Optional<Edge> edgeOpt = this.parent.metadata.getEdge(edgeId);
			if (edgeOpt.isPresent()) {
				Edge edge = edgeOpt.get();
				EdgeInfo info = new EdgeInfo(edge.isOnline());
				result.put(edge.getId(), info);
			}
		}
		return CompletableFuture.completedFuture(new GetStatusOfEdgesResponse(messageId, result));
	}

	/**
	 * Handles a GetChannelsValuesRequest.
	 * 
	 * @param user      the User
	 * @param messageId the JSON-RPC Message-ID
	 * @param request   the GetChannelsValuesRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetChannelsValuesRequest(User user, UUID messageId,
			GetChannelsValuesRequest request) throws OpenemsNamedException {
		GetChannelsValuesResponse response = new GetChannelsValuesResponse(messageId);
		for (String edgeId : request.getEdgeIds()) {
			for (ChannelAddress channel : request.getChannels()) {
				Optional<JsonElement> value = this.parent.timeData.getChannelValue(edgeId, channel);
				response.addValue(edgeId, channel, value.orElse(JsonNull.INSTANCE));
			}
		}
		return CompletableFuture.completedFuture(response);
	}

	/**
	 * Handles a SetGridConnScheduleRequest.
	 * 
	 * @param user                       the User
	 * @param messageId                  the JSON-RPC Message-ID
	 * @param setGridConnScheduleRequest the SetGridConnScheduleRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSetGridConnScheduleRequest(User user, UUID messageId,
			SetGridConnScheduleRequest setGridConnScheduleRequest) throws OpenemsNamedException {
		user.assertEdgeRoleIsAtLeast("SetGridConnSchedule", setGridConnScheduleRequest.getEdgeId(), Role.ADMIN);

		// wrap original request inside ComponentJsonApiRequest
		String componentId = "ctrlBalancingSchedule0"; // TODO find dynamic Component-ID of BalancingScheduleController
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, setGridConnScheduleRequest);

		CompletableFuture<JsonrpcResponseSuccess> resultFuture = this.parent.edgeWebsocket
				.send(setGridConnScheduleRequest.getEdgeId(), request);

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
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(messageId, result.toJsonObject()));
	}

}
