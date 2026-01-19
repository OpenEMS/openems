package io.openems.backend.edge.client;

import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.backend.common.edge.jsonrpc.UpdateMetadataCache;
import io.openems.backend.edge.application.Cache;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.BooleanConsumer;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.ClientReconnectorWorker;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.OnOpen;
import io.openems.common.websocket.WsData;

public class WebsocketClient extends AbstractWebsocketClient<WsData> {

	private final Logger log = LoggerFactory.getLogger(WebsocketClient.class);
	private final ThreadPoolExecutor executor;

	private final OnNotification onNotification;
	private final OnRequest onRequest;
	private final OnError onError;

	/**
	 * Builds a {@link WebsocketClient}.
	 * 
	 * @param name                   a human readble name
	 * @param uri                    the connection Uri to OpenEMS Backend
	 *                               Edge-Manager
	 * @param id                     unique ID of this Backend Edge Application
	 * @param poolSize               number of threads to handle tasks
	 * @param onConnectedChange      callback for connection to Edge-Manager status
	 *                               changes
	 * @param sendRequestToEdge      method to send a {@link JsonrpcRequest} to an
	 *                               Edge
	 * @param sendNotificationToEdge method to send a {@link JsonrpcNotification} to
	 *                               an Edge
	 * @param updateCache            callback for a {@link Cache} update
	 */
	public WebsocketClient(//
			String name, URI uri, String id, int poolSize, //
			BooleanConsumer onConnectedChange, //
			BiFunction<String, JsonrpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>> sendRequestToEdge, //
			BiConsumer<String, JsonrpcNotification> sendNotificationToEdge, //
			Consumer<UpdateMetadataCache.Notification> updateCache) {
		super(name, uri, Map.of("id", id), onConnectedChange,
				new ClientReconnectorWorker.Config(100, 30, 2, 30 * 1000 /* 30 seconds */));
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize,
				new ThreadFactoryBuilder().setNameFormat("Backend.Edge.App-%d").build());
		this.onNotification = new OnNotification(//
				name, //
				sendNotificationToEdge, //
				updateCache, //
				this::logWarn);
		this.onRequest = new OnRequest(//
				name, //
				sendRequestToEdge);
		this.onError = new OnError(//
				this::logError);
	}

	/**
	 * Deactivate and stop the {@link WebsocketClient}.
	 */
	public void deactivate() {
		shutdownAndAwaitTermination(this.executor, 0);
		this.stop();
	}

	/**
	 * Sends a {@link JsonrpcRequest} to the Edge-Manager.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param request the {@link JsonrpcRequest}
	 * @return a promise for a successful JSON-RPC Response
	 */
	public CompletableFuture<JsonrpcResponseSuccess> sendRequestToEdgeManager(String edgeId, JsonrpcRequest request) {
		var responseFuture = this.sendRequest(new EdgeRpcRequest(edgeId, request));

		// Unwrap Response
		var result = new CompletableFuture<JsonrpcResponseSuccess>();
		responseFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				try {
					var response = getAsJsonObject(getAsJsonObject(r.getResult(), "payload"), "result");
					result.complete(new GenericJsonrpcResponseSuccess(request.id, response));
				} catch (OpenemsNamedException e) {
					this.logError(this.log, e.getMessage());
					result.completeExceptionally(e);
				}
			} else {
				result.completeExceptionally(
						new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD, request.getMethod()));
			}
		});
		return result;
	}

	/**
	 * Sends a {@link JsonrpcNotification} to the Edge-Manager.
	 * 
	 * @param edgeId       the Edge-ID
	 * @param notification the {@link JsonrpcNotification}
	 */
	public void sendNotificationToEdgeManager(String edgeId, JsonrpcNotification notification) {
		this.sendMessage(new EdgeRpcNotification(edgeId, notification));
	}

	@Override
	public OnOpen getOnOpen() {
		return OnOpen.NO_OP;
	}

	@Override
	public OnNotification getOnNotification() {
		return this.onNotification;
	}

	@Override
	protected OnRequest getOnRequest() {
		return this.onRequest;
	}

	@Override
	public OnError getOnError() {
		return this.onError;
	}

	@Override
	public OnClose getOnClose() {
		return OnClose.NO_OP;
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		log.info("[" + this.getName() + "] " + message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		log.warn("[" + this.getName() + "] " + message);
	}

	@Override
	protected void logError(Logger log, String message) {
		log.error("[" + this.getName() + "] " + message);
	}

	public boolean isConnected() {
		return this.ws.isOpen();
	}

	@Override
	protected void execute(Runnable command) {
		this.executor.execute(command);
	}
}
