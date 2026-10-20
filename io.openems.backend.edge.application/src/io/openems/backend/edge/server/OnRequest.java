package io.openems.backend.edge.server;

import static io.openems.common.utils.JsonrpcUtils.simplifyJsonrpcMessage;
import static io.openems.common.utils.StringUtils.toShortString;
import static java.util.concurrent.CompletableFuture.failedFuture;

import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.edge.application.BackendEdgeServerApp;
import io.openems.backend.edge.syncable.StaticSyncableSubscriptionHandler;
import io.openems.backend.metrics.prometheus.PrometheusMetrics;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SyncableSubscribeRequest;
import io.openems.common.jsonrpc.request.SyncableUnsubscribeRequest;
import io.openems.common.jsonrpc.response.SyncableSubscribeResponse;
import io.openems.common.jsonrpc.syncable.SyncableProviderType;
import io.prometheus.metrics.core.datapoints.Timer;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);

	private final String name;
	private final BackendEdgeServerApp app;

	public OnRequest(//
			String name, //
			BackendEdgeServerApp app) {
		this.name = name;
		this.app = app;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> apply(WebSocket ws, JsonrpcRequest request)
			throws OpenemsError.OpenemsNamedException {
		final var timer = PrometheusMetrics.WEBSOCKET_REQUEST.labelValues(this.name, request.getFullyQualifiedMethod())
				.startTimer();

		WsData wsData = ws.getAttachment();
		var edgeId = wsData.getEdgeId();
		if (edgeId == null) {
			timer.close();
			return failedFuture(OpenemsError.JSONRPC_SEND_FAILED.exception());
		}

		wsData.debugLog(this.log,
				() -> "REQUEST " + edgeId + ":" + toShortString(simplifyJsonrpcMessage(request), 200));

		return addTimerClose(() -> switch (request.getMethod()) {
		case SyncableSubscribeRequest.METHOD ->
			this.handleSyncableSubscribe(SyncableSubscribeRequest.from(request), request, wsData);
		case SyncableUnsubscribeRequest.METHOD ->
			this.handleSyncableUnsubscribe(SyncableUnsubscribeRequest.from(request), request, wsData);
		default -> this.app.sendRequestToEdgeManager(edgeId, request);
		}, timer);
	}

	private static CompletableFuture<? extends JsonrpcResponseSuccess> addTimerClose(
			ThrowingSupplier<CompletableFuture<? extends JsonrpcResponseSuccess>, Exception> futureFunc, Timer timer) {
		try {
			final var future = futureFunc.get();
			future.whenComplete((t, u) -> timer.close());
			return future;
		} catch (Exception e) {
			timer.close();
			return failedFuture(e);
		}
	}

	private CompletableFuture<? extends JsonrpcResponseSuccess> handleSyncableSubscribe(
			SyncableSubscribeRequest request, JsonrpcRequest rawRequest, WsData wsData) {

		return switch (request.getKey().getProviderType()) {
		case SyncableProviderType.BACKEND_EDGE_SERVER -> {
			var subscription = this.app.getSyncableManager().subscribe(request.getKey(), wsData);

			var handler = (StaticSyncableSubscriptionHandler) subscription.globalHandler();
			var currentData = (handler != null) ? handler.getTrackedEntry() : null;
			var response = new SyncableSubscribeResponse(rawRequest.getId(), currentData);
			yield CompletableFuture.completedFuture(response);
		}
		case SyncableProviderType.BACKEND -> //
			this.app.sendRequestToEdgeManager(wsData.getEdgeId(), rawRequest);
		};
	}

	private CompletableFuture<? extends JsonrpcResponseSuccess> handleSyncableUnsubscribe(
			SyncableUnsubscribeRequest request, JsonrpcRequest rawRequest, WsData wsData) {

		return switch (request.getKey().getProviderType()) {
		case SyncableProviderType.BACKEND_EDGE_SERVER -> {
			this.app.getSyncableManager().unsubscribe(request.getKey(), wsData);
			yield CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
		}
		case SyncableProviderType.BACKEND -> //
			this.app.sendRequestToEdgeManager(wsData.getEdgeId(), rawRequest);
		};
	}

}
