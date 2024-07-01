package io.openems.backend.core.jsonrpcrequesthandler;

import static io.openems.common.utils.JsonUtils.getAsOptionalBoolean;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.common.utils.JsonUtils.getAsString;
import static java.util.Collections.emptyMap;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import io.openems.backend.common.metadata.AppCenterHandler;
import io.openems.backend.common.metadata.Metadata.GenericSystemLog;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AppCenterRequest;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyPerPeriodRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesExportXlxsRequest;
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

		if (user.getRole(edgeId).isEmpty()) {
			this.parent.metadata.getEdgeMetadataForUser(user, edgeId);
		}
		user.assertEdgeRoleIsAtLeast(EdgeRpcRequest.METHOD, edgeRpcRequest.getEdgeId(), Role.GUEST);

		var resultFuture = switch (request.getMethod()) {
		case AppCenterRequest.METHOD -> AppCenterHandler.handleUserRequest(this.parent.appCenterMetadata, //
				t -> this.handleRequest(user, messageId, t), //
				AppCenterRequest.from(request), user, edgeId);

		case QueryHistoricTimeseriesDataRequest.METHOD ->
			this.handleQueryHistoricDataRequest(edgeId, user, QueryHistoricTimeseriesDataRequest.from(request));

		case QueryHistoricTimeseriesEnergyRequest.METHOD ->
			this.handleQueryHistoricEnergyRequest(edgeId, user, QueryHistoricTimeseriesEnergyRequest.from(request));

		case QueryHistoricTimeseriesEnergyPerPeriodRequest.METHOD -> this.handleQueryHistoricEnergyPerPeriodRequest(
				edgeId, user, QueryHistoricTimeseriesEnergyPerPeriodRequest.from(request));

		case QueryHistoricTimeseriesExportXlxsRequest.METHOD -> this.handleQueryHistoricTimeseriesExportXlxsRequest(
				edgeId, user, QueryHistoricTimeseriesExportXlxsRequest.from(request));

		case GetEdgeConfigRequest.METHOD ->
			this.handleGetEdgeConfigRequest(edgeId, user, GetEdgeConfigRequest.from(request));

		case ComponentJsonApiRequest.METHOD -> {
			final var componentRequest = ComponentJsonApiRequest.from(request);
			if (!"_host".equals(componentRequest.getComponentId())) {
				yield null;
			}
			switch (componentRequest.getPayload().getMethod()) {
			case "executeSystemCommand" -> {
				final var executeSystemCommandRequest = componentRequest.getPayload();
				final var p = executeSystemCommandRequest.getParams();
				this.parent.metadata.logGenericSystemLog(new LogSystemExecuteCommend(edgeId, user, //
						getAsString(p, "command"), //
						getAsOptionalBoolean(p, "sudo").orElse(null), //
						getAsOptionalString(p, "username").orElse(null), //
						getAsOptionalString(p, "password").orElse(null), //
						getAsOptionalBoolean(p, "runInBackground").orElse(null) //
				));
			}
			case "executeSystemUpdate" -> {
				this.parent.metadata.logGenericSystemLog(new LogUpdateSystem(edgeId, user));
			}
			case "executeSystemRestart" -> {
				final var executeSystemCommandRequest = componentRequest.getPayload();
				final var p = executeSystemCommandRequest.getParams();
				this.parent.metadata.logGenericSystemLog(new LogRestartSystem(edgeId, user, //
						getAsOptionalString(p, "type").orElse(null) //
				));
			}
			}
			yield null;
		}

		default -> null;
		};

		if (resultFuture == null) {
			// Request not handled delegate to edge
			resultFuture = this.parent.edgeWebsocket.send(edgeId, user, request);
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

	private record LogSystemExecuteCommend(//
			String edgeId, // non-null
			User user, // non-null
			String command, // non-null
			Boolean sudo, // null-able
			String username, // null-able
			String password, // null-able
			Boolean runInBackground // null-able
	) implements GenericSystemLog {

		@Override
		public String teaser() {
			return "Systemcommand: " + this.command;
		}

		@Override
		public Map<String, String> getValues() {
			return Map.of(//
					"Sudo", Boolean.toString(Optional.ofNullable(this.sudo()).orElse(false)), //
					"Command", this.command(), //
					"Username", this.username(), //
					"Password", this.password() == null || this.password().isEmpty() ? "[NOT_SET]" : "[SET]", //
					"Run in Background", Boolean.toString(Optional.ofNullable(this.runInBackground()).orElse(false)) //
			);
		}

	}

	private record LogUpdateSystem(//
			String edgeId, // non-null
			User user // non-null
	) implements GenericSystemLog {

		@Override
		public String teaser() {
			return "Systemupdate";
		}

		@Override
		public Map<String, String> getValues() {
			return emptyMap();
		}

	}

	private record LogRestartSystem(//
			String edgeId, // non-null
			User user, // non-null
			String type // null-able
	) implements GenericSystemLog {

		@Override
		public String teaser() {
			return "Systemrestart";
		}

		@Override
		public Map<String, String> getValues() {
			if (this.type == null) {
				return emptyMap();
			}
			return Map.of(//
					"type", this.type //
			);
		}

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

}
