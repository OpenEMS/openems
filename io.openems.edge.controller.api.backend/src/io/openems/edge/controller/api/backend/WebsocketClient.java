package io.openems.edge.controller.api.backend;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.OnClose;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.controller.api.backend.BackendApi.ChannelId;

public class WebsocketClient extends AbstractWebsocketClient<WsData> {

	private final Logger log = LoggerFactory.getLogger(WebsocketClient.class);

	private final BackendApi parent;
	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	protected WebsocketClient(BackendApi parent, String name, URI serverUri, Map<String, String> httpHeaders,
			Proxy proxy) {
		super(name, serverUri, httpHeaders, proxy);
		this.parent = parent;
		this.onOpen = new OnOpen(parent);
		this.onRequest = new OnRequest(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = (ws, code, reason, remote) -> {
			this.parent.channel(ChannelId.BACKEND_CONNECTED).setNextValue(false);
			BooleanReadChannel status = this.parent.channel(ChannelId.BACKEND_CONNECTED);
			log.info("Backend Connected Channel: " + status.getNextValue().get());
			log.error("Disconnected from OpenEMS Backend [" + serverUri.toString() + //
			(proxy != AbstractWebsocketClient.NO_PROXY ? " via Proxy" : "") + "]");
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
	protected WsData createWsData() {
		return new WsData();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		this.parent.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		this.parent.logWarn(log, message);
	}
}
