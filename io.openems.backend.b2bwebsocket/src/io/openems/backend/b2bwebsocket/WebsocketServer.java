package io.openems.backend.b2bwebsocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.common.websocket.AbstractWebsocketServer;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final Backend2BackendWebsocket parent;
	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	public WebsocketServer(Backend2BackendWebsocket parent, String name, int port, int poolSize) {
		super(name, port, poolSize);
		this.parent = parent;
		this.onOpen = new OnOpen(//
				() -> parent.metadata, //
				this::logInfo);
		this.onRequest = new OnRequest(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = new OnClose(parent);
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws, this.parent);
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
