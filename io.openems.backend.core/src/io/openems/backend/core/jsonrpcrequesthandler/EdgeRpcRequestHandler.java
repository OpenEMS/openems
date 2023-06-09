package io.openems.backend.core.jsonrpcrequesthandler;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
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
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.response.GetEdgeConfigResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyPerPeriodResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyResponse;
import io.openems.common.session.Role;

public class EdgeRpcRequestHandler {

	private final CoreJsonRpcRequestHandlerImpl parent;

	protected EdgeRpcRequestHandler(CoreJsonRpcRequestHandlerImpl parent) {
		this.parent = parent;
	}

	/**
	 * Handles an {@link EdgeRpcRequest}.
	 *
	 * @param user           the {@link User}
	 * @param edgeRpcRequest the {@link EdgeRpcRequest}
	 * @param messageId      the JSON-RPC Message-ID
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	protected CompletableFuture<EdgeRpcResponse> handleRequest(User user, UUID messageId, EdgeRpcRequest edgeRpcRequest)
			throws OpenemsNamedException {
		var edgeId = edgeRpcRequest.getEdgeId();
		var request = edgeRpcRequest.getPayload();
		user.assertEdgeRoleIsAtLeast(EdgeRpcRequest.METHOD, edgeRpcRequest.getEdgeId(), Role.GUEST);
	
		CompletableFuture<JsonrpcResponseSuccess> resultFuture;
		
		  resultFuture = switch (request.getMethod()) {

	     	case QueryHistoricTimeseriesDataRequest.METHOD -> 
	     		this.handleQueryHistoricDataRequest(edgeId, user,QueryHistoricTimeseriesDataRequest.from(request));								

		    case QueryHistoricTimeseriesEnergyRequest.METHOD -> 
		    	this.handleQueryHistoricEnergyRequest(edgeId, user,QueryHistoricTimeseriesEnergyRequest.from(request));
	
		    case QueryHistoricTimeseriesEnergyPerPeriodRequest.METHOD ->
		    	this.handleQueryHistoricEnergyPerPeriodRequest(edgeId, user,QueryHistoricTimeseriesEnergyPerPeriodRequest.from(request));
			
		    case QueryHistoricTimeseriesExportXlxsRequest.METHOD ->
		    	this.handleQueryHistoricTimeseriesExportXlxsRequest(edgeId, user,QueryHistoricTimeseriesExportXlxsRequest.from(request));	
		    	
		    case GetEdgeConfigRequest.METHOD ->
		    	this.handleGetEdgeConfigRequest(edgeId, user, GetEdgeConfigRequest.from(request));
			
		    case CreateComponentConfigRequest.METHOD ->
		    	this.handleCreateComponentConfigRequest(edgeId, user,CreateComponentConfigRequest.from(request));		
		    	
		    case UpdateComponentConfigRequest.METHOD -> 
		    	this.handleUpdateComponentConfigRequest(edgeId, user,UpdateComponentConfigRequest.from(request));
		    	
		    case DeleteComponentConfigRequest.METHOD ->
		    	this.handleDeleteComponentConfigRequest(edgeId, user,DeleteComponentConfigRequest.from(request));
		    	
		    case SetChannelValueRequest.METHOD -> 
		    	this.handleSetChannelValueRequest(edgeId, user, SetChannelValueRequest.from(request));
			
		    case ComponentJsonApiRequest.METHOD -> 
		    	this.handleComponentJsonApiRequest(edgeId, user, ComponentJsonApiRequest.from(request));
		
		    default ->
		    	throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
			
		};

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
	 * Handles a {@link QueryHistoricTimeseriesDataRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User} - no specific level required
	 * @param request the {@link QueryHistoricTimeseriesDataRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricDataRequest(String edgeId, User user,
			QueryHistoricTimeseriesDataRequest request) throws OpenemsNamedException {
		var historicData = this.parent.timedataManager.queryHistoricData(edgeId, request);

		// JSON-RPC response
		return CompletableFuture
				.completedFuture(new QueryHistoricTimeseriesDataResponse(request.getId(), historicData));
	}

	/**
	 * Handles a {@link QueryHistoricTimeseriesEnergyRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User} - no specific level required
	 * @param request the {@link QueryHistoricTimeseriesEnergyRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricEnergyRequest(String edgeId, User user,
			QueryHistoricTimeseriesEnergyRequest request) throws OpenemsNamedException {
		var data = this.parent.timedataManager.queryHistoricEnergy(//
				edgeId, request.getFromDate(), request.getToDate(), request.getChannels());

		// JSON-RPC response
		return CompletableFuture.completedFuture(new QueryHistoricTimeseriesEnergyResponse(request.getId(), data));
	}

	/**
	 * Handles a {@link QueryHistoricTimeseriesEnergyPerPeriodRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User} - no specific level required
	 * @param request the {@link QueryHistoricTimeseriesEnergyPerPeriodRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricEnergyPerPeriodRequest(String edgeId,
			User user, QueryHistoricTimeseriesEnergyPerPeriodRequest request) throws OpenemsNamedException {
		var data = this.parent.timedataManager.queryHistoricEnergyPerPeriod(//
				edgeId, request.getFromDate(), request.getToDate(), request.getChannels(), request.getResolution());

		// JSON-RPC response
		return CompletableFuture
				.completedFuture(new QueryHistoricTimeseriesEnergyPerPeriodResponse(request.getId(), data));
	}

	/**
	 * Handles a {@link QueryHistoricTimeseriesExportXlxsRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User}
	 * @param request the {@link QueryHistoricTimeseriesExportXlxsRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleQueryHistoricTimeseriesExportXlxsRequest(String edgeId,
			User user, QueryHistoricTimeseriesExportXlxsRequest request) throws OpenemsNamedException {
		return CompletableFuture.completedFuture(this.parent.timedataManager
				.handleQueryHistoricTimeseriesExportXlxsRequest(edgeId, request, user.getLanguage()));
	}

	/**
	 * Handles a {@link GetEdgeConfigRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User} - no specific level required
	 * @param request the {@link GetEdgeConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(String edgeId, User user,
			GetEdgeConfigRequest request) throws OpenemsNamedException {
		var config = this.parent.metadata.edge().getEdgeConfig(edgeId);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GetEdgeConfigResponse(request.getId(), config));
	}

	/**
	 * Handles a {@link CreateComponentConfigRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User} - Installer-level required
	 * @param request the {@link CreateComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleCreateComponentConfigRequest(String edgeId, User user,
			CreateComponentConfigRequest request) throws OpenemsNamedException {
		user.assertEdgeRoleIsAtLeast(CreateComponentConfigRequest.METHOD, edgeId, Role.INSTALLER);

		return this.parent.edgeWebsocket.send(edgeId, user, request);
	}

	/**
	 * Handles a {@link UpdateComponentConfigRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User} - Installer-level required
	 * @param request the {@link UpdateComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleUpdateComponentConfigRequest(String edgeId, User user,
			UpdateComponentConfigRequest request) throws OpenemsNamedException {
		user.assertEdgeRoleIsAtLeast(UpdateComponentConfigRequest.METHOD, edgeId, Role.OWNER);

		return this.parent.edgeWebsocket.send(edgeId, user, request);
	}

	/**
	 * Handles a {@link DeleteComponentConfigRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User} - Installer-level required
	 * @param request the {@link DeleteComponentConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleDeleteComponentConfigRequest(String edgeId, User user,
			DeleteComponentConfigRequest request) throws OpenemsNamedException {
		user.assertEdgeRoleIsAtLeast(DeleteComponentConfigRequest.METHOD, edgeId, Role.INSTALLER);

		return this.parent.edgeWebsocket.send(edgeId, user, request);
	}

	/**
	 * Handles a {@link SetChannelValueRequest}.
	 *
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User}
	 * @param request the {@link SetChannelValueRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(String edgeId, User user,
			SetChannelValueRequest request) throws OpenemsNamedException {
		user.assertEdgeRoleIsAtLeast(SetChannelValueRequest.METHOD, edgeId, Role.ADMIN);

		return this.parent.edgeWebsocket.send(edgeId, user, request);
	}

	/**
	 * Handles a {@link UpdateComponentConfigRequest}.
	 *
	 * @param edgeId                  the Edge-ID
	 * @param user                    the {@link User} - Guest-level required
	 * @param componentJsonApiRequest the {@link ComponentJsonApiRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleComponentJsonApiRequest(String edgeId, User user,
			ComponentJsonApiRequest componentJsonApiRequest) throws OpenemsNamedException {
		user.assertEdgeRoleIsAtLeast(ComponentJsonApiRequest.METHOD, edgeId, Role.GUEST);

		return this.parent.edgeWebsocket.send(edgeId, user, componentJsonApiRequest);
	}
}
