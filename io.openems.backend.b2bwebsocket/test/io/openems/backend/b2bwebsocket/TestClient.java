package io.openems.backend.b2bwebsocket;

import java.net.URI;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.OnError;
import io.openems.common.websocket.OnNotification;
import io.openems.common.websocket.OnOpen;
import io.openems.common.websocket.OnRequest;
import io.openems.common.websocket.WsData;

public class TestClient extends AbstractWebsocketClient<WsData> {

	private final Logger log = LoggerFactory.getLogger(TestClient.class);

	private OnOpen onOpen;
	private OnRequest onRequest;
	private OnNotification onNotification;
	private OnError onError;
	private OnClose onClose;

	protected TestClient(URI serverUri, Map<String, String> httpHeaders) {
		super("B2bwebsocket.Unittest", serverUri, httpHeaders);
		this.onOpen = (ws, handshake) -> {
			this.log.info("OnOpen: " + handshake);
			return null;
		};
		this.onRequest = (ws, request) -> {
			this.log.info("OnRequest: " + request);
			return null;
		};
		this.onNotification = (ws, notification) -> {
			this.log.info("OnNotification: " + notification);
		};
		this.onError = (ws, ex) -> {
			this.log.info("onError: " + ex.getMessage());
		};
		this.onClose = (ws, code, reason, remote) -> {
			this.log.info("onClose: " + reason);
		};
	}

	@Override
	public OnOpen getOnOpen() {
		return this.onOpen;
	}

	public void setOnOpen(OnOpen onOpen) {
		this.onOpen = onOpen;
	}

	@Override
	public OnRequest getOnRequest() {
		return this.onRequest;
	}

	public void setOnRequest(OnRequest onRequest) {
		this.onRequest = onRequest;
	}

	@Override
	public OnError getOnError() {
		return this.onError;
	}

	public void setOnError(OnError onError) {
		this.onError = onError;
	}

	@Override
	public OnClose getOnClose() {
		return this.onClose;
	}

	public void setOnClose(OnClose onClose) {
		this.onClose = onClose;
	}

	@Override
	protected OnNotification getOnNotification() {
		return this.onNotification;
	}

	public void setOnNotification(OnNotification onNotification) {
		this.onNotification = onNotification;
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws) {
			@Override
			public String toString() {
				return "TestClient.WsData []";
			}
		};
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
	protected void logError(Logger log, String message) {
		log.error(message);
	}

	@Override
	protected void execute(Runnable command) {
		command.run();
	}
}
