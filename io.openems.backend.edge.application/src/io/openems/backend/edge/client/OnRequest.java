package io.openems.backend.edge.client;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.java_websocket.WebSocket;

import io.openems.backend.metrics.prometheus.PrometheusMetrics;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final String name;
	private final BiFunction<String, JsonrpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>> sendRequestToEdge;

	public OnRequest(//
			String name, //
			BiFunction<String, JsonrpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>> sendRequestToEdge //
	) {
		this.name = name;
		this.sendRequestToEdge = sendRequestToEdge;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> apply(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		final var timer = PrometheusMetrics.WEBSOCKET_REQUEST.labelValues(this.name, request.getFullyQualifiedMethod())
				.startTimer();
		try {
			final var response = switch (request.getMethod()) {
			case EdgeRpcRequest.METHOD //
				-> this.handleEdgeRpcRequest(EdgeRpcRequest.from(request));
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
	 * @param request the {@link EdgeRpcRequest}
	 * @return a {@link CompletableFuture}
	 */
	private CompletableFuture<? extends JsonrpcResponseSuccess> handleEdgeRpcRequest(EdgeRpcRequest request)
			throws OpenemsNamedException {
		final var edgeId = request.getEdgeId();
		final var payload = request.getPayload();
		var responseFuture = this.sendRequestToEdge.apply(edgeId, payload);

		// Wrap Response
		var result = new CompletableFuture<JsonrpcResponseSuccess>();
		responseFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				result.complete(new EdgeRpcResponse(request.id, r));
			} else {
				result.completeExceptionally(
						new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD, request.getMethod()));
			}
		});
		return result;
	}
}
