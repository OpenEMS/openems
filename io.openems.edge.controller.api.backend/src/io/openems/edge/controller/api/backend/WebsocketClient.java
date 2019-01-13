package io.openems.edge.controller.api.backend;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.OnInternalError;

public class WebsocketClient extends AbstractWebsocketClient<WsData> {

	private final Logger log = LoggerFactory.getLogger(WebsocketClient.class);

	private final BackendApi parent;
	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;
	private final OnInternalError onInternalError;

	protected WebsocketClient(BackendApi parent, String name, URI serverUri, Map<String, String> httpHeaders,
			Proxy proxy) {
		super(name, serverUri, httpHeaders, proxy);
		this.parent = parent;
		this.onOpen = new OnOpen(parent);
		this.onRequest = new OnRequest(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = (ws, code, reason, remote) -> {
			log.error("Disconnected from OpenEMS Backend [" + serverUri.toString() + //
			(proxy != AbstractWebsocketClient.NO_PROXY ? " via Proxy" : "") + "]");
		};
		this.onInternalError = (ex) -> {
			ex.printStackTrace();
		};
	}

	@Override
	public OnOpen getOnOpen() {
		return this.onOpen;
	}

	@Override
	public OnRequest getOnRequest() {
		return this.onRequest;
	}

	@Override
	public OnNotification getOnNotification() {
		return onNotification;
	}

	@Override
	public OnError getOnError() {
		return this.onError;
	}

	@Override
	public OnClose getOnClose() {
		return this.onClose;
	}

	@Override
	public OnInternalError getOnInternalError() {
		return this.onInternalError;
	}

	@Override
	protected WsData createWsData() {
		return new WsData();
	}
	
	@Override
	protected void logWarn(Logger log, String message) {
		this.parent.logWarn(log, message);
	}
}
