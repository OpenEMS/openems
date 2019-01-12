package io.openems.backend.b2bwebsocket;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.OnError;
import io.openems.common.websocket.OnInternalError;
import io.openems.common.websocket.OnNotification;
import io.openems.common.websocket.OnOpen;
import io.openems.common.websocket.OnRequest;
import io.openems.common.websocket.WsData;

public class TestClient extends AbstractWebsocketClient<WsData> {

	private Logger log = LoggerFactory.getLogger(TestClient.class);

	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;
	private final OnInternalError onInternalError;

	protected TestClient(URI serverUri) {
		super("B2bwebsocket.Unittest", serverUri);
		this.onOpen = (ws, handshake) -> {
			log.info("OnOpen: " + handshake);
		};
		this.onRequest = (ws, request) -> {
			log.info("OnRequest: " + request);
			return null;
		};
		this.onNotification = (ws, notification) -> {
			log.info("OnNotification: " + notification);
		};
		this.onError = (ws, ex) -> {
			log.info("onError: " + ex.getMessage());
		};
		this.onClose = (ws, code, reason, remote) -> {
			log.info("onClose: " + reason);
		};
		this.onInternalError = (ex) -> {
			log.warn("onInternalError. " + ex.getClass() + ": " + ex.getMessage());
			ex.printStackTrace();
		};
	}

	@Override
	public OnOpen getOnOpen() {
		return onOpen;
	}

	@Override
	public OnRequest getOnRequest() {
		return onRequest;
	}

	@Override
	public OnError getOnError() {
		return onError;
	}

	@Override
	public OnClose getOnClose() {
		return onClose;
	}

	@Override
	public OnInternalError getOnInternalError() {
		return onInternalError;
	}

	@Override
	protected OnNotification getOnNotification() {
		return onNotification;
	}

	@Override
	protected WsData createWsData() {
		return new WsData();
	}

	@Override
	protected void logWarn(Logger log, String message) {
		log.warn(message);
	}
}
