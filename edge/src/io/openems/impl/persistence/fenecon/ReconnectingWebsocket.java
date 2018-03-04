package io.openems.impl.persistence.fenecon;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocket.READYSTATE;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.App;
import io.openems.api.security.User;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.utilities.Mutex;
import io.openems.core.utilities.websocket.EdgeWebsocketHandler;

public class ReconnectingWebsocket {

	private final Logger log = LoggerFactory.getLogger(ReconnectingWebsocket.class);
	private final int DEFAULT_WAIT_AFTER_CLOSE = 1; // 1 second
	private final int MAX_WAIT_AFTER_CLOSE = 60 * 3; // 3 minutes
	private int WAIT_AFTER_CLOSE = DEFAULT_WAIT_AFTER_CLOSE;
	private final Draft WEBSOCKET_DRAFT = new Draft_6455();
	private final Mutex WEBSOCKET_CLOSED = new Mutex(true);
	private Optional<URI> uriOpt = Optional.empty();
	private Optional<Proxy> proxyOpt = Optional.empty();
	private final Map<String, String> httpHeaders = new HashMap<>();
	private final OnOpenListener ON_OPEN_LISTENER;
	private final OnCloseListener ON_CLOSE_LISTENER;
	private Optional<MyWebSocketClient> WEBSOCKET_OPT = Optional.empty();

	private final ScheduledExecutorService reconnectorExecutor = Executors
			.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Re-Ws-%d").build());
	private final ScheduledExecutorService waitAfterCloseExecutor = Executors
			.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Re-WC-%d").build());
	private ScheduledFuture<?> reconnectorFuture = null;
	private final Runnable reconnectorTask;

	/**
	 * Local implementation of WebSocketClient to catch events
	 *
	 * @author stefan.feilmeier
	 */
	private class MyWebSocketClient extends WebSocketClient {
		private final EdgeWebsocketHandler handler ;

		public MyWebSocketClient(URI uri, Map<String, String> httpHeaders, Proxy proxy) throws IOException {
			this(uri, httpHeaders);
			this.setProxy(proxy);
		}

		public MyWebSocketClient(URI uri, Map<String, String> httpHeaders) throws IOException {
			super(uri, WEBSOCKET_DRAFT, httpHeaders, 0);
			EdgeWebsocketHandler handler = new EdgeWebsocketHandler(this);
			handler.setUser(User.ADMIN);
			this.handler = handler;
		}

		@Override
		public void onOpen(ServerHandshake handshakedata) {
			log.info("Websocket [" + this.getURI().toString() + "] opened");
			ON_OPEN_LISTENER.announce(this);
			WAIT_AFTER_CLOSE = DEFAULT_WAIT_AFTER_CLOSE;
		}

		@Override
		public void onMessage(String message) {
			try {
				JsonObject jMessage = (new JsonParser()).parse(message).getAsJsonObject();
				this.handler.onMessage(jMessage);
			} catch (OutOfMemoryError e) {
				// Java-Websocket library can cause an "unable to create new native thread" OutOfMemoryError on
				// subscribe. We are not able to recover that.
				App.shutdownWithError("ReconnectingWebsocket. Error on message [" + message + "]", e);
			} catch (Throwable t) {
				log.error("Websocket [" + this.getURI().toString() + "] error on message [" + message + "]: "
						+ t.getMessage());
				t.printStackTrace();
			}
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			log.info("Websocket [" + this.getURI().toString() + "] closed. Code [" + code + "] Reason [" + reason
					+ "] Wait [" + WAIT_AFTER_CLOSE + "]");
			WAIT_AFTER_CLOSE += DEFAULT_WAIT_AFTER_CLOSE * 2;
			if (WAIT_AFTER_CLOSE > MAX_WAIT_AFTER_CLOSE) {
				WAIT_AFTER_CLOSE = MAX_WAIT_AFTER_CLOSE;
			}
			waitAfterCloseExecutor.schedule(() -> {
				WEBSOCKET_CLOSED.release(); // trigger reconnector
			}, WAIT_AFTER_CLOSE, TimeUnit.SECONDS);
			ON_CLOSE_LISTENER.announce();
		}

		@Override
		public void onError(Exception ex) {
			log.warn("Websocket [" + this.getURI().toString() + "] error: " + ex.getMessage());
		}

		@Override
		protected void finalize() throws Throwable {
			this.handler.dispose();
			super.finalize();
		}

		protected void sendLog(long timestamp, String level, String source, String message) {
			this.handler.sendLog(timestamp, level, source, message);
		}
	}

	@FunctionalInterface
	public interface OnOpenListener {
		public void announce(WebSocket websocket);
	}

	@FunctionalInterface
	public interface OnCloseListener {
		public void announce();
	}

	/**
	 * Builds a reconnecting websocket. Does nothing till setUri() is called.
	 *
	 * @param uri
	 * @param httpHeaders
	 */
	public ReconnectingWebsocket(OnOpenListener onOpenListener, OnCloseListener onCloseListener) {
		this.ON_OPEN_LISTENER = onOpenListener;
		this.ON_CLOSE_LISTENER = onCloseListener;
		this.reconnectorTask = () -> {
			while (true) {
				try {
					// wait for websocket close or check once every 5 minutes
					WEBSOCKET_CLOSED.awaitOrTimeout(5, TimeUnit.MINUTES);

					if (WEBSOCKET_OPT.isPresent()) {
						WebSocket ws = WEBSOCKET_OPT.get();
						if (ws.getReadyState() == READYSTATE.OPEN || ws.getReadyState() == READYSTATE.CONNECTING) {
							// Websocket is available and connected or just connecting -> nothing to do
							continue;

						} else {
							// Websocket died -> remove it
							WEBSOCKET_OPT = Optional.empty();
						}
					}

					if (!uriOpt.isPresent()) {
						// uri is not available. stop here
						continue;
					}

					// Create new websocket and open connection
					MyWebSocketClient ws;
					if (this.proxyOpt.isPresent()) {
						ws = new MyWebSocketClient(uriOpt.get(), httpHeaders, this.proxyOpt.get());
					} else {
						ws = new MyWebSocketClient(uriOpt.get(), httpHeaders);
					}
					ws.connect();
					WEBSOCKET_OPT = Optional.of(ws);

				} catch (Throwable t) {
					String wsString = uriOpt.isPresent() ? uriOpt.get().toString() : "NO_URI";
					log.error("Websocket [" + wsString + "] reconnect error. " + t.getClass().getSimpleName() + ": " + t.getMessage());
				}
			}
		};
	}

	/**
	 * Sets the Websocket URI and optional proxy settings and starts the websocket
	 *
	 * @param uriOpt
	 */
	public void setUri(Optional<URI> uriOpt, Optional<Proxy> proxyOpt) {
		this.uriOpt = uriOpt;
		this.proxyOpt = proxyOpt;
		if (this.reconnectorFuture != null) {
			this.reconnectorFuture.cancel(true);
		}
		this.reconnectorFuture = this.reconnectorExecutor.scheduleWithFixedDelay(this.reconnectorTask, 0, 1,
				TimeUnit.SECONDS);
	}

	/**
	 * Add a header.
	 *
	 * @param key
	 * @param value
	 */
	public void addHttpHeader(String key, String value) {
		// this is not able to handle changes after websocket was established
		this.httpHeaders.put(key, value);
	}

	public void dispose() {
		this.reconnectorExecutor.shutdownNow();
	}

	public boolean websocketIsOpen() {
		if (WEBSOCKET_OPT.isPresent()) {
			WebSocket ws = WEBSOCKET_OPT.get();
			if (ws.getReadyState() == READYSTATE.OPEN) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Send message to websocket
	 *
	 * @param j
	 * @return
	 * @throws OpenemsException
	 */
	protected void send(JsonObject j) throws OpenemsException {
		if (WEBSOCKET_OPT.isPresent()) {
			WebSocketUtils.send(WEBSOCKET_OPT.get(), j);
		} else {
			throw new OpenemsException("No Websocket!");
		}
	}

	public void sendLog(long timestamp, String level, String source, String message) {
		if(this.WEBSOCKET_OPT.isPresent()) {
			this.WEBSOCKET_OPT.get().sendLog(timestamp, level, source, message);
		}
	}
}
