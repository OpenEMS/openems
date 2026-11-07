package io.openems.common.websocket;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Simple {@link AbstractWebsocketClient} for tests.
 */
public class TestWebsocketClient extends AbstractWebsocketClient<WsData> implements AutoCloseable {

	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnClose onClose;

	public TestWebsocketClient(URI uri, Map<String, String> httpHeaders, ClientReconnectorWorker.Config config,
			OnOpen onOpen, OnRequest onRequest, OnNotification onNotification, OnClose onClose) {
		super("TestWebsocketClient", uri, httpHeaders, null /* onConnectedChange */, config);
		this.onOpen = onOpen;
		this.onRequest = onRequest;
		this.onNotification = onNotification;
		this.onClose = onClose;
	}

	public boolean isConnected() {
		return this.ws.isOpen();
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws);
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
	protected OnNotification getOnNotification() {
		return this.onNotification;
	}

	@Override
	protected OnError getOnError() {
		return OnError.NO_OP;
	}

	@Override
	protected OnClose getOnClose() {
		return this.onClose;
	}

	@Override
	protected void execute(Runnable command) {
		Thread.ofVirtual().start(command);
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
	public void close() {
		this.stop();
	}
}
