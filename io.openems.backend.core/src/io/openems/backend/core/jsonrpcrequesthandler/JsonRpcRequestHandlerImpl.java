package io.openems.backend.core.jsonrpcrequesthandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.jsonrpc.JsonRpcRequestHandler;
import io.openems.backend.common.jsonrpc.request.GetEdgesChannelsValuesRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesStatusRequest;
import io.openems.backend.common.jsonrpc.response.GetEdgesChannelsValuesResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse.EdgeInfo;
import io.openems.backend.edgewebsocket.api.EdgeWebsocket;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.timedata.api.Timedata;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.session.Role;
import io.openems.common.session.User;
import io.openems.common.types.ChannelAddress;

@Component(//
		name = "Core.JsonRpcRequestHandler", //
		immediate = true //
)
public class JsonRpcRequestHandlerImpl extends AbstractOpenemsBackendComponent implements JsonRpcRequestHandler {

	private final Logger log = LoggerFactory.getLogger(JsonRpcRequestHandler.class);

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected volatile EdgeWebsocket edgeWebsocket;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected volatile Metadata metadata;

	@Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
	protected volatile Timedata timeData;

	private final EdgeRpcRequestHandler edgeRpcRequestHandler;

	public JsonRpcRequestHandlerImpl() {
		super("Core.JsonRpcRequestHandler");
		this.edgeRpcRequestHandler = new EdgeRpcRequestHandler(this);
	}

	/**
	 * Handles a JSON-RPC Request.
	 * 
	 * @param context the Logger context, i.e. the name of the parent source
	 * @param user    the User
	 * @param request the JsonrpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleRequest(String context, BackendUser user,
			JsonrpcRequest request) throws OpenemsNamedException {
		switch (request.getMethod()) {

		case EdgeRpcRequest.METHOD:
			return this.edgeRpcRequestHandler.handleRequest(user, request.getId(), EdgeRpcRequest.from(request));

		case GetEdgesStatusRequest.METHOD:
			return this.handleGetStatusOfEdgesRequest(user, request.getId(), GetEdgesStatusRequest.from(request));

		case GetEdgesChannelsValuesRequest.METHOD:
			return this.handleGetChannelsValuesRequest(user, request.getId(),
					GetEdgesChannelsValuesRequest.from(request));

		case SetGridConnScheduleRequest.METHOD:
			return this.handleSetGridConnScheduleRequest(user, request.getId(),
					SetGridConnScheduleRequest.from(request));

		default:
			this.logWarn(context, "Unhandled Request: " + request);
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
	private CompletableFuture<GetEdgesStatusResponse> handleGetStatusOfEdgesRequest(BackendUser user, UUID messageId,
			GetEdgesStatusRequest request) throws OpenemsNamedException {
		Map<String, EdgeInfo> result = new HashMap<>();
		for (Entry<String, Role> entry : user.getEdgeRoles().entrySet()) {
			String edgeId = entry.getKey();

			// assure read permissions of this User for this Edge.
			if (!user.edgeRoleIsAtLeast(edgeId, Role.GUEST)) {
				continue;
			}

			Optional<Edge> edgeOpt = this.metadata.getEdge(edgeId);
			if (edgeOpt.isPresent()) {
				Edge edge = edgeOpt.get();
				EdgeInfo info = new EdgeInfo(edge.isOnline());
				result.put(edge.getId(), info);
			}
		}
		return CompletableFuture.completedFuture(new GetEdgesStatusResponse(messageId, result));
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
	private CompletableFuture<GetEdgesChannelsValuesResponse> handleGetChannelsValuesRequest(BackendUser user,
			UUID messageId, GetEdgesChannelsValuesRequest request) throws OpenemsNamedException {
		GetEdgesChannelsValuesResponse response = new GetEdgesChannelsValuesResponse(messageId);
		for (String edgeId : request.getEdgeIds()) {
			// assure read permissions of this User for this Edge.
			if (!user.edgeRoleIsAtLeast(edgeId, Role.GUEST)) {
				continue;
			}

			for (ChannelAddress channel : request.getChannels()) {
				Optional<JsonElement> value = this.timeData.getChannelValue(edgeId, channel);
				response.addValue(edgeId, channel, value.orElse(JsonNull.INSTANCE));
			}
		}
		return CompletableFuture.completedFuture(response);
	}

	/**
	 * Handles a SetGridConnScheduleRequest.
	 * 
	 * @param backendUser                the User
	 * @param messageId                  the JSON-RPC Message-ID
	 * @param setGridConnScheduleRequest the SetGridConnScheduleRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleSetGridConnScheduleRequest(BackendUser backendUser,
			UUID messageId, SetGridConnScheduleRequest setGridConnScheduleRequest) throws OpenemsNamedException {
		String edgeId = setGridConnScheduleRequest.getEdgeId();
		User user = backendUser.getAsCommonUser(edgeId);
		user.assertRoleIsAtLeast(SetGridConnScheduleRequest.METHOD, Role.ADMIN);

		// wrap original request inside ComponentJsonApiRequest
		String componentId = "ctrlBalancingSchedule0"; // TODO find dynamic Component-ID of BalancingScheduleController
		ComponentJsonApiRequest request = new ComponentJsonApiRequest(componentId, setGridConnScheduleRequest);

		CompletableFuture<JsonrpcResponseSuccess> resultFuture = this.edgeWebsocket.send(edgeId, user, request);

		// Wrap reply in GenericJsonrpcResponseSuccess
		CompletableFuture<GenericJsonrpcResponseSuccess> result = new CompletableFuture<GenericJsonrpcResponseSuccess>();
		resultFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				result.complete(new GenericJsonrpcResponseSuccess(messageId, r.toJsonObject()));
			} else {
				result.completeExceptionally(new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD,
						SetGridConnScheduleRequest.METHOD));
			}
		});
		return result;
	}

	/**
	 * Log an info message including the Handler name.
	 * 
	 * @param context the Logger context, i.e. the name of the parent source
	 * @param message the Info-message
	 */
	protected void logInfo(String context, String message) {
		this.log.info("[" + context + "] " + message);
	}

	/**
	 * Log a warn message including the Handler name.
	 * 
	 * @param context the Logger context, i.e. the name of the parent source
	 * @param message the Warn-message
	 */
	protected void logWarn(String context, String message) {
		this.log.warn("[" + context + "] " + message);
	}

	/**
	 * Log an error message including the Handler name.
	 * 
	 * @param context the Logger context, i.e. the name of the parent source
	 * @param message the Error-message
	 */
	protected void logError(String context, String message) {
		this.log.error("[" + context + "] " + message);
	}
}
