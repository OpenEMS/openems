package io.openems.edge.controller.api.backend;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.websocket_old.WebSocketUtils;
import io.openems.edge.controller.api.core.ApiController;
import io.openems.edge.controller.api.core.EdgeWebsocketHandler;

/**
 * Local implementation of WebSocketClient to catch events
 *
 * @author stefan.feilmeier
 */
class MyWebSocketClient extends WebSocketClient {

	private final static Draft WEBSOCKET_DRAFT = new Draft_6455();
	private final static int DEFAULT_WAIT_AFTER_CLOSE = 5; // 1 second
	private final static int MAX_WAIT_AFTER_CLOSE = 60 * 3; // 3 minutes

	private final Logger log = LoggerFactory.getLogger(MyWebSocketClient.class);
	private final EdgeWebsocketHandler handler;
	private final Consumer<WebSocket> onOpenCallback;
	private final Runnable onCloseCallback;
	private final ScheduledExecutorService reconnectExecutor = Executors
			.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Re-WC-%d").build());

	private int waitAfterClose = DEFAULT_WAIT_AFTER_CLOSE;

	public MyWebSocketClient(ApiController parent, URI uri, Map<String, String> httpHeaders, Optional<Proxy> proxy,
			Consumer<WebSocket> onOpenCallback, Runnable onCloseCallback) {
		super(uri, WEBSOCKET_DRAFT, httpHeaders, 0);
		if (proxy.isPresent()) {
			this.setProxy(proxy.get());
		}
		this.onOpenCallback = onOpenCallback;
		this.onCloseCallback = onCloseCallback;
		EdgeWebsocketHandler handler = new EdgeWebsocketHandler(parent, this);
		handler.setRole(io.openems.common.session.Role.ADMIN);
		this.handler = handler;
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		this.log.info("Websocket [" + this.getURI().toString() + "] opened");
		this.onOpenCallback.accept(this);
	}

	@Override
	public void onMessage(String message) {
		try {
			JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();
			this.handler.onMessage(jMessage);

			// reset wait after close
			this.waitAfterClose = DEFAULT_WAIT_AFTER_CLOSE;
		} catch (Throwable t) {
			this.log.error("Websocket [" + this.getURI().toString() + "] error on message [" + message + "]: "
					+ t.getMessage());
			t.printStackTrace();
		}
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		this.waitAfterClose += DEFAULT_WAIT_AFTER_CLOSE * 2;
		if (this.waitAfterClose > MAX_WAIT_AFTER_CLOSE) {
			this.waitAfterClose = MAX_WAIT_AFTER_CLOSE;
		}
		this.log.info("Websocket [" + this.getURI().toString() + "] closed. Code [" + code + "] Reason [" + reason
				+ "] Wait [" + this.waitAfterClose + "]");
		this.reconnectExecutor.schedule(() -> {
			this.reconnect();
		}, this.waitAfterClose, TimeUnit.SECONDS);
		this.onCloseCallback.run();
	}

	@Override
	public void onError(Exception ex) {
		this.log.warn("Websocket [" + this.getURI().toString() + "] error: " + ex.getMessage());
	}

	protected void deactivate() {
		this.close(2000, "Disabled Backend Api Controller");
		this.handler.dispose();
		this.reconnectExecutor.shutdownNow();
		try {
			this.reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			this.log.error("Unable to shutdown: " + e.getMessage());
		}
	}

	protected void sendLog(PaxLoggingEvent event) {
		this.handler.sendLog(event);
	}

	/**
	 * Send message to websocket
	 *
	 * @param j
	 * @return
	 * @throws OpenemsException
	 */
	protected void send(JsonObject j) throws OpenemsException {
		WebSocketUtils.send(this, j);
	}
}