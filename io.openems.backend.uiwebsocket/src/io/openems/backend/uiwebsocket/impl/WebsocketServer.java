package io.openems.backend.uiwebsocket.impl;

import java.time.Instant;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.common.websocket.AbstractWebsocketServer;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final Logger log = LoggerFactory.getLogger(WebsocketServer.class);

	protected final UiWebsocketImpl parent;
	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	public WebsocketServer(UiWebsocketImpl parent, String name, int port, int poolSize, DebugMode debugMode) {
		super(name, port, poolSize, debugMode, (executor) -> {
			// Store Metrics
			var data = TreeBasedTable.<Long, String, JsonElement>create();
			var now = Instant.now().toEpochMilli();
			ThreadPoolUtils.debugMetrics(executor).forEach((key, value) -> {
				data.put(now, "uiwebsocket/" + key, new JsonPrimitive(value));
			});
			parent.timedataManager.write("backend0", new TimestampedDataNotification(data));
		});
		this.parent = parent;
		this.onOpen = new OnOpen(parent);
		this.onRequest = new OnRequest(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = new OnClose(parent);
	}

	@Override
	protected WsData createWsData() {
		return new WsData(this);
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
	protected JsonrpcMessage handleNonJsonrpcMessage(WebSocket ws, String stringMessage,
			OpenemsNamedException lastException) throws OpenemsNamedException {
		this.log.info("UiWs. handleNonJsonrpcMessage: " + stringMessage);
		throw new OpenemsException("UiWs. handleNonJsonrpcMessage", lastException);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		this.parent.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		this.parent.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		this.parent.logError(log, message);
	}
}
