package io.openems.backend.core.jsonrpcrequesthandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.backend.common.edgewebsocket.EdgeWebsocket;
import io.openems.backend.common.jsonrpc.JsonRpcRequestHandler;
import io.openems.backend.common.jsonrpc.request.GetEdgesChannelsValuesRequest;
import io.openems.backend.common.jsonrpc.request.GetEdgesStatusRequest;
import io.openems.backend.common.jsonrpc.response.GetEdgesChannelsValuesResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse;
import io.openems.backend.common.jsonrpc.response.GetEdgesStatusResponse.EdgeInfo;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.SetGridConnScheduleRequest;
import io.openems.common.session.Role;

@Designate(ocd = Config.class, factory = false)
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
	protected volatile TimedataManager timedataManager;

	private final EdgeRpcRequestHandler edgeRpcRequestHandler;
	protected Config config;

	public JsonRpcRequestHandlerImpl() {
		super("Core.JsonRpcRequestHandler");
		this.edgeRpcRequestHandler = new EdgeRpcRequestHandler(this);
	}

	@Activate
	private void activate(Config config) {
		this.updateConfig(config);
	}

	@Modified
	private void modified(Config config) {
		this.updateConfig(config);
	}

	private void updateConfig(Config config) {
		this.config = config;
	}

	/**
	 * Handles a JSON-RPC Request.
	 *
	 * @param context the Logger context, i.e. the name of the parent source
	 * @param user    the {@link User}
	 * @param request the JsonrpcRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleRequest(String context, User user,
			JsonrpcRequest request) throws OpenemsNamedException {
		
		return switch (request.getMethod()) {

				case EdgeRpcRequest.METHOD -> 
					this.edgeRpcRequestHandler.handleRequest(user, request.getId(), EdgeRpcRequest.from(request));			 

				case GetEdgesStatusRequest.METHOD -> 
					this.handleGetEdgesStatusRequest(user, request.getId(), GetEdgesStatusRequest.from(request));

				case GetEdgesChannelsValuesRequest.METHOD -> 
					this.handleGetChannelsValuesRequest(user, request.getId(),GetEdgesChannelsValuesRequest.from(request));
					
				case SetGridConnScheduleRequest.METHOD -> 
					this.handleSetGridConnScheduleRequest(user, request.getId(),SetGridConnScheduleRequest.from(request));
					
				default -> {
					this.logWarn(context, "Unhandled Request: " + request);
					throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
				}
		};
	}


	/**
	 * Handles a {@link GetEdgesStatusRequest}.
	 *
	 * @param user      the {@link User}
	 * @param messageId the JSON-RPC Message-ID
	 * @param request   the {@link GetEdgesStatusRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GetEdgesStatusResponse> handleGetEdgesStatusRequest(User user, UUID messageId,
			GetEdgesStatusRequest request) throws OpenemsNamedException {
		Map<String, EdgeInfo> result = new HashMap<>();
		for (Entry<String, Role> entry : user.getEdgeRoles().entrySet()) {
			var edgeId = entry.getKey();

			// assure read permissions of this User for this Edge.
			user.assertEdgeRoleIsAtLeast(GetEdgesStatusRequest.METHOD, edgeId, Role.GUEST);

			var edgeOpt = this.metadata.getEdge(edgeId);
			if (edgeOpt.isPresent()) {
				var edge = edgeOpt.get();
				var info = new EdgeInfo(edge.isOnline());
				result.put(edge.getId(), info);
			}
		}
		return CompletableFuture.completedFuture(new GetEdgesStatusResponse(messageId, result));
	}

	/**
	 * Handles a {@link GetEdgesChannelsValuesRequest}.
	 *
	 * @param user      the {@link User}
	 * @param messageId the JSON-RPC Message-ID
	 * @param request   the GetChannelsValuesRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GetEdgesChannelsValuesResponse> handleGetChannelsValuesRequest(User user, UUID messageId,
			GetEdgesChannelsValuesRequest request) throws OpenemsNamedException {
		var response = new GetEdgesChannelsValuesResponse(messageId);
		for (String edgeId : request.getEdgeIds()) {
			// assure read permissions of this User for this Edge.
			user.assertEdgeRoleIsAtLeast(GetEdgesStatusRequest.METHOD, edgeId, Role.GUEST);

			var data = this.edgeWebsocket.getChannelValues(edgeId, request.getChannels());
			for (var entry : data.entrySet()) {
				response.addValue(edgeId, entry.getKey(), entry.getValue());
			}
		}
		return CompletableFuture.completedFuture(response);
	}

	/**
	 * Handles a {@link SetGridConnScheduleRequest}.
	 *
	 * @param user                       the {@link User}
	 * @param messageId                  the JSON-RPC Message-ID
	 * @param setGridConnScheduleRequest the {@link SetGridConnScheduleRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleSetGridConnScheduleRequest(User user, UUID messageId,
			SetGridConnScheduleRequest setGridConnScheduleRequest) throws OpenemsNamedException {
		var edgeId = setGridConnScheduleRequest.getEdgeId();
		user.assertEdgeRoleIsAtLeast(SetGridConnScheduleRequest.METHOD, edgeId, Role.ADMIN);

		// wrap original request inside ComponentJsonApiRequest
		var componentId = "ctrlBalancingSchedule0"; // TODO find dynamic Component-ID of BalancingScheduleController
		var request = new ComponentJsonApiRequest(componentId, setGridConnScheduleRequest);

		var resultFuture = this.edgeWebsocket.send(edgeId, user, request);

		// Wrap reply in GenericJsonrpcResponseSuccess
		var result = new CompletableFuture<GenericJsonrpcResponseSuccess>();
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
