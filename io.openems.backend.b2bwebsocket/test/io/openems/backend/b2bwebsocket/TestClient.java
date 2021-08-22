package io.openems.backend.b2bwebsocket;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.DummyWsData;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.OnError;
import io.openems.common.websocket.OnNotification;
import io.openems.common.websocket.OnOpen;
import io.openems.common.websocket.OnRequest;
import io.openems.common.websocket.WsData;

public class TestClient extends AbstractWebsocketClient<WsData> {

	private Logger log = LoggerFactory.getLogger(TestClient.class);

	private OnOpen onOpen;
	private OnRequest onRequest;
	private OnNotification onNotification;
	private OnError onError;
	private OnClose onClose;

	protected TestClient(URI serverUri, Map<String, String> httpHeaders) {
		super("B2bwebsocket.Unittest", serverUri, httpHeaders);
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
	}

	@Override
	public OnOpen getOnOpen() {
		return onOpen;
	}

	public void setOnOpen(OnOpen onOpen) {
		this.onOpen = onOpen;
	}

	@Override
	public OnRequest getOnRequest() {
		return onRequest;
	}

	public void setOnRequest(OnRequest onRequest) {
		this.onRequest = onRequest;
	}

	@Override
	public OnError getOnError() {
		return onError;
	}

	public void setOnError(OnError onError) {
		this.onError = onError;
	}

	@Override
	public OnClose getOnClose() {
		return onClose;
	}

	public void setOnClose(OnClose onClose) {
		this.onClose = onClose;
	}

	@Override
	protected OnNotification getOnNotification() {
		return onNotification;
	}

	public void setOnNotification(OnNotification onNotification) {
		this.onNotification = onNotification;
	}

	@Override
	protected WsData createWsData() {
		return new DummyWsData();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		log.info(message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		log.warn(message);
	}

	@Override
	protected void execute(Runnable command) {
		command.run();
	}
}
