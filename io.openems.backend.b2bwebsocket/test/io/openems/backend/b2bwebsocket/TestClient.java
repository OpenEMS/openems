package io.openems.backend.b2bwebsocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
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

public class TestClient extends AbstractWebsocketClient<WsData> implements AutoCloseable {

	private final Logger log = LoggerFactory.getLogger(TestClient.class);

	private OnOpen onOpen;
	private OnRequest onRequest;
	private OnNotification onNotification;
	private OnError onError;
	private OnClose onClose;

	/**
	 * Prepares and starts a TestClient with the given URI, username, and password.
	 * 
	 * @param uri      the URI of the WebSocket server
	 * @param username the username for authentication
	 * @param password the password for authentication
	 * @return a TestClient instance that is started and ready to use
	 * @throws URISyntaxException   if the URI is malformed
	 * @throws InterruptedException if the thread is interrupted while starting the
	 *                              client
	 */
	public static TestClient prepareAndStart(String uri, String username, String password)
			throws URISyntaxException, InterruptedException {
		Map<String, String> httpHeaders = new HashMap<>();
		var auth = new String(Base64.getEncoder().encode((username + ":" + password).getBytes()),
				StandardCharsets.UTF_8);
		httpHeaders.put("Authorization", "Basic " + auth);
		var client = new TestClient(new URI(uri), httpHeaders);
		client.startBlocking();
		return client;
	}

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
	public void close() {
		this.stop();
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
		return new WsData(ws);
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
