package io.openems.backend.edge.server;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.websocket.AbstractWebsocketServer;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	public WebsocketServer(String name, int port, int poolSize, //
			BiFunction<String, JsonrpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>> sendRequestToEdgeManager, //
			BiConsumer<String, JsonrpcNotification> sendNotificationToEdgeManager, //
			Function<String, String> authenticateApikey, //
			Runnable connectedEdgesChanged) {
		super(name, port, poolSize);
		this.onOpen = new OnOpen(//
				authenticateApikey, //
				connectedEdgesChanged);
		this.onRequest = new OnRequest(//
				name, //
				sendRequestToEdgeManager);
		this.onNotification = new OnNotification(//
				name, //
				sendNotificationToEdgeManager);
		this.onError = new OnError(//
				this::logError);
		this.onClose = new OnClose(//
				connectedEdgesChanged);
	}

	/**
	 * Sends a {@link JsonrpcRequest} to an Edge.
	 * 
	 * @param edgeId  the Edge-ID
	 * @param request the {@link JsonrpcRequest}
	 * @return a promise for a successful JSON-RPC Response
	 */
	public CompletableFuture<JsonrpcResponseSuccess> sendRequestToEdge(String edgeId, JsonrpcRequest request) {
		var wsData = this.getWsDataForEdgeId(edgeId);
		if (wsData == null) {
			return CompletableFuture.failedFuture(OpenemsError.JSONRPC_SEND_FAILED.exception());
		}
		return wsData.send(request);
	}

	/**
	 * Sends a {@link JsonrpcNotification} to an Edge.
	 * 
	 * @param edgeId       the Edge-ID
	 * @param notification the {@link JsonrpcNotification}
	 */
	public void sendNotificationToEdge(String edgeId, JsonrpcNotification notification) {
		var wsData = this.getWsDataForEdgeId(edgeId);
		if (wsData == null) {
			return; // No connection for this Edge. Ignore.
		}
		wsData.send(notification);
	}

	/**
	 * Gets the {@link WsData} for the given Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @return {@link WsData} or null
	 */
	private WsData getWsDataForEdgeId(String edgeId) {
		return this.getConnections().stream() //
				.map(c -> (WsData) c.getAttachment()) //
				.filter(w -> Objects.equals(w.getEdgeId(), edgeId)) //
				.findFirst().orElse(null);
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws);
	}

	@Override
	protected OnOpen getOnOpen() {
		return this.onOpen;
	}

	@Override
	protected OnRequest getOnRequest() {
		return this.onRequest;
	}

	@Override
	public OnNotification getOnNotification() {
		return this.onNotification;
	}

	@Override
	protected OnError getOnError() {
		return this.onError;
	}

	@Override
	protected OnClose getOnClose() {
		return this.onClose;
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
}
