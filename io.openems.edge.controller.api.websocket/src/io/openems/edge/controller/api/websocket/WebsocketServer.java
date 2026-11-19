package io.openems.edge.controller.api.websocket;

import java.util.concurrent.RejectedExecutionException;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;

import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.OnOpen;
import io.openems.common.websocket.OnRequest;
import io.openems.edge.controller.api.websocket.externalauth.ExternalAuthentication;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final ControllerApiWebsocketImpl parent;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;
	private final ExternalAuthentication externalAuthentication;

	public WebsocketServer(ControllerApiWebsocketImpl parent, String name, int port, int poolSize,
			ExternalAuthentication externalAuthentication) {
		super(name, port, poolSize);
		this.parent = parent;
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = new OnClose(parent);
		this.externalAuthentication = externalAuthentication;
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws, this.parent);
	}

	@Override
	protected WsData onHandshake(WebSocket ws, Draft draft, ClientHandshake request) throws InvalidDataException {
		final var user = this.externalAuthentication.resolveUser(ws.getRemoteSocketAddress().getAddress(), request)
				.orElse(null);
		return new WsData(ws, this.parent, user);
	}

	@Override
	protected OnOpen getOnOpen() {
		return OnOpen.NO_OP;
	}

	@Override
	protected OnRequest getOnRequest() {
		return this.parent.onRequest;
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

	@Override
	protected void execute(Runnable command) throws RejectedExecutionException {
		super.execute(command);
	}
}
