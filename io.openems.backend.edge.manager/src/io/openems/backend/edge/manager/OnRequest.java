package io.openems.backend.edge.manager;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.AppCenterHandler;
import io.openems.backend.common.metadata.AppCenterMetadata;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.metrics.prometheus.PrometheusMetrics;
import io.openems.backend.oauthregistry.OAuthRegistry;
import io.openems.backend.oauthregistry.OAuthRegistryRequestHandler;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AppCenterRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.OAuthRegistryRequest;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.jsonrpc.type.CheckSetupPassword;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);

	private final String name;
	private final Supplier<AppCenterMetadata.EdgeData> appCenterMetadata;
	private final Supplier<OAuthRegistry> oauthRegistry;
	private final Function<String, Optional<Edge>> getEdgeIdBySetupPassword;
	private final BiConsumer<Logger, String> logWarn;

	public OnRequest(//
			String name, //
			Supplier<AppCenterMetadata.EdgeData> appCenterMetadata, //
			Supplier<OAuthRegistry> oauthRegistry, //
			Function<String, Optional<String>> getEdgeIdForApikey, //
			Function<String, Optional<Edge>> getEdgeIdBySetupPassword, //
			Function<String, Optional<Edge>> getEdge, //
			BiConsumer<Logger, String> logWarn) {
		this.name = name;
		this.appCenterMetadata = appCenterMetadata;
		this.oauthRegistry = oauthRegistry;
		this.getEdgeIdBySetupPassword = getEdgeIdBySetupPassword;
		this.logWarn = logWarn;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> apply(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		final var timer = PrometheusMetrics.WEBSOCKET_REQUEST.labelValues(this.name, request.getFullyQualifiedMethod())
				.startTimer();
		try {
			final var response = switch (request.getMethod()) {
			case EdgeRpcRequest.METHOD //
				-> this.handleEdgeRpcRequest(EdgeRpcRequest.from(request), ws.getAttachment());
			default //
				-> throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
			};

			return response.whenComplete((t, u) -> {
				timer.close();
			});
		} catch (Exception e) {
			timer.close();
			throw e;
		}
	}

	/**
	 * Handles a {@link EdgeRpcRequest}.
	 *
	 * @param edgeRpcRequest the {@link EdgeRpcRequest}
	 * @param wsData         the {@link WsData}
	 * @return a {@link CompletableFuture}
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleEdgeRpcRequest(EdgeRpcRequest edgeRpcRequest,
			WsData wsData) throws OpenemsNamedException {
		final var edgeId = edgeRpcRequest.getEdgeId();
		final var request = edgeRpcRequest.getPayload();

		CompletableFuture<? extends JsonrpcResponseSuccess> resultFuture = switch (request.getMethod()) {
		case AppCenterRequest.METHOD -> //
			AppCenterHandler.handleEdgeRequest(this.appCenterMetadata.get(), AppCenterRequest.from(request), edgeId);
		case CheckSetupPassword.METHOD -> this.handleCheckSetupPasswordRequest(edgeId, request);
		case OAuthRegistryRequest.METHOD ->
			OAuthRegistryRequestHandler.handleRequest(this.oauthRegistry.get(), OAuthRegistryRequest.from(request));
		default -> {
			this.logWarn.accept(this.log, "Unhandled Request: " + request);
			yield CompletableFuture.failedFuture(OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod()));
		}
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

	private CompletableFuture<? extends JsonrpcResponseSuccess> handleCheckSetupPasswordRequest(String edgeId,
			JsonrpcRequest requestRaw) {
		final var request = CheckSetupPassword.Request.serializer().deserialize(requestRaw.getParams());

		return this.getEdgeIdBySetupPassword.apply(request.setupPassword()) //
				.map(t -> {
					if (!t.getId().equals(edgeId)) {
						return null;
					}

					return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(requestRaw.getId()));
				}) //
				.orElseGet(() -> CompletableFuture.failedFuture(
						new OpenemsNamedException(OpenemsError.COMMON_AUTHENTICATION_FAILED, requestRaw.getMethod())));
	}

}
