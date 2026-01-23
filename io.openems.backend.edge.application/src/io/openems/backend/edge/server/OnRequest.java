package io.openems.backend.edge.server;

import static io.openems.common.utils.JsonrpcUtils.simplifyJsonrpcMessage;
import static io.openems.common.utils.StringUtils.toShortString;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metrics.prometheus.PrometheusMetrics;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);

	private final String name;
	private final BiFunction<String, JsonrpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>> sendToEdgeManager;

	public OnRequest(//
			String name, //
			BiFunction<String, JsonrpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>> sendToEdgeManager) {
		this.name = name;
		this.sendToEdgeManager = sendToEdgeManager;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> apply(WebSocket ws, JsonrpcRequest request) {
		final var timer = PrometheusMetrics.WEBSOCKET_REQUEST.labelValues(this.name, request.getFullyQualifiedMethod())
				.startTimer();

		WsData wsData = ws.getAttachment();
		var edgeId = wsData.getEdgeIdWithTimeout(10, SECONDS);
		if (edgeId == null) {
			timer.close();
			return failedFuture(OpenemsError.JSONRPC_SEND_FAILED.exception());
		}

		wsData.debugLog(this.log,
				() -> "REQUEST " + edgeId + ":" + toShortString(simplifyJsonrpcMessage(request), 200));
		return this.sendToEdgeManager.apply(edgeId, request).whenComplete((t, u) -> {
			timer.close();
		});
	}

}
