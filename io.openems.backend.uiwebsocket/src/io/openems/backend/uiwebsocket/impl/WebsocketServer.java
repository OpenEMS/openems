package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.OnOpen;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	protected final UiWebsocketImpl parent;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;

	public WebsocketServer(UiWebsocketImpl parent, String name, int port, int poolSize) {
		super(name, port, poolSize);
		this.parent = parent;
		this.onRequest = new OnRequest(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws);
	}

	@Override
	protected OnOpen getOnOpen() {
		return OnOpen.NO_OP;
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
		return OnClose.NO_OP;
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
