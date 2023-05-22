package io.openems.backend.b2bwebsocket;

import java.time.Instant;

import org.slf4j.Logger;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.utils.ThreadPoolUtils;
import io.openems.common.websocket.AbstractWebsocketServer;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final B2bWebsocket parent;
	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	public WebsocketServer(B2bWebsocket parent, String name, int port, int poolSize, DebugMode debugMode) {
		super(name, port, poolSize, debugMode, (executor) -> {
			// Store Metrics
			var data = TreeBasedTable.<Long, String, JsonElement>create();
			var now = Instant.now().toEpochMilli();
			ThreadPoolUtils.debugMetrics(executor).forEach((key, value) -> {
				data.put(now, "b2bwebsocket/" + key, new JsonPrimitive(value));
			});
			parent.timedataManager.write("backend0", data);
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
		return new WsData(this.parent);
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
