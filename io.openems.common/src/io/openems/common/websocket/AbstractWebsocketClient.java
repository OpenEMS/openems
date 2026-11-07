package io.openems.common.websocket;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.client.Socks5Proxy;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.core.Configuration;
import org.eclipse.jetty.websocket.core.client.CoreClientUpgradeRequest;
import org.eclipse.jetty.websocket.core.client.UpgradeListener;
import org.eclipse.jetty.websocket.core.client.WebSocketCoreClient;
import org.eclipse.jetty.websocket.core.exception.UpgradeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.function.BooleanConsumer;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.FunctionUtils;

/**
 * A Websocket Client implementation that automatically tries to reconnect a
 * closed connection.
 *
 * @param <T> the type of websocket attachments inheriting {@link WsData}
 */
public abstract class AbstractWebsocketClient<T extends WsData> extends AbstractWebsocket<T> {

	public static final Map<String, String> NO_HTTP_HEADERS = new HashMap<>();
	public static final Proxy NO_PROXY = null;

	/**
	 * Interval for pings to detect lost connections.
	 */
	private static final Duration CONNECTION_LOST_TIMEOUT = Duration.ofSeconds(100);

	/**
	 * Max time to wait for the close handshake on {@link #stop()}.
	 */
	private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(2);

	/**
	 * Threads for Jetty I/O; message handling uses {@link #execute(Runnable)}.
	 */
	private static final int MAX_JETTY_THREADS = 8;

	/**
	 * The {@link WebSocket}; it is kept across reconnects.
	 */
	protected final WebSocket ws;

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketClient.class);
	private final JettyWebSocket websocket;
	private final URI serverUri;
	private final Map<String, String> httpHeaders;
	private final Proxy proxy;
	private final BooleanConsumer onConnectedChange;
	private final AtomicBoolean isConnected = new AtomicBoolean(false);
	private final AtomicInteger connectAttempts = new AtomicInteger();
	private final ClientReconnectorWorker.Config reconnectorConfig;
	private final ClientReconnectorWorker reconnectorWorker;

	private WebSocketCoreClient client;

	protected AbstractWebsocketClient(String name, URI serverUri) {
		this(name, serverUri, AbstractWebsocketClient.NO_HTTP_HEADERS, AbstractWebsocketClient.NO_PROXY,
				null /* onConnectedChange */);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders) {
		this(name, serverUri, httpHeaders, AbstractWebsocketClient.NO_PROXY, null /* onConnectedChange */);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders,
			BooleanConsumer onConnectedChange) {
		this(name, serverUri, httpHeaders, AbstractWebsocketClient.NO_PROXY, onConnectedChange);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders,
			BooleanConsumer onConnectedChange, ClientReconnectorWorker.Config reconnectorConfig) {
		this(name, serverUri, httpHeaders, AbstractWebsocketClient.NO_PROXY, onConnectedChange, reconnectorConfig);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders, Proxy proxy) {
		this(name, serverUri, httpHeaders, proxy, null /* onConnectedChange */);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders, Proxy proxy,
			BooleanConsumer onConnectedChange) {
		this(name, serverUri, httpHeaders, proxy, onConnectedChange, ClientReconnectorWorker.DEFAULT_CONFIG);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders, Proxy proxy,
			BooleanConsumer onConnectedChange, ClientReconnectorWorker.Config reconnectorConfig) {
		super(name);
		this.serverUri = serverUri;
		this.httpHeaders = Map.copyOf(httpHeaders);
		this.proxy = proxy;
		this.onConnectedChange = onConnectedChange == null ? FunctionUtils::doNothing : onConnectedChange;
		this.websocket = new JettyWebSocket(null);
		this.ws = this.websocket;

		// initialize WsData
		this.websocket.setAttachment(this.createWsData(this.ws));

		// Initialize reconnector
		this.reconnectorConfig = reconnectorConfig;
		this.reconnectorWorker = new ClientReconnectorWorker(this, reconnectorConfig);
	}

	/**
	 * Called before the handshake request is sent to the server.
	 *
	 * @param request the HTTP headers of the request
	 */
	protected void onWebsocketHandshakeSent(Handshake request) {
		// nothing
	}

	/**
	 * Starts the websocket client.
	 */
	@Override
	public void start() {
		this.logInfo(this.log, "Opening connection to websocket server [" + this.serverUri + "]");
		this.reconnectorWorker.activate(this.getName() + "::Reconnector");
		this.reconnectorWorker.triggerNextRun();
	}

	/**
	 * Starts the websocket client; waiting till it is connected.
	 *
	 * @throws InterruptedException on waiting error
	 */
	public void startBlocking() throws InterruptedException {
		this.logInfo(this.log, "Opening connection to websocket server [" + this.serverUri + "]");
		this.connect(this.reconnectorConfig.connectTimeoutSeconds());
		this.reconnectorWorker.activate(this.getName() + "::Reconnector");
	}

	/**
	 * Stops the websocket client.
	 */
	@Override
	public void stop() {
		this.logInfo(this.log, "Closing connection to websocket server [" + this.serverUri + "]");
		// shutdown reconnector
		this.reconnectorWorker.deactivate();
		// close websocket
		this.websocket.closeAndWait(CloseCode.NORMAL, "Closing connection [" + this.getName() + "]",
				CLOSE_TIMEOUT);
		this.stopClient();
	}

	/**
	 * Tries to connect to the server.
	 *
	 * @param timeoutSeconds the max time to wait for the connection
	 * @return true if connected
	 * @throws InterruptedException on waiting error
	 */
	protected synchronized boolean connect(int timeoutSeconds) throws InterruptedException {
		if (this.ws.isOpen()) {
			return true;
		}
		if (this.connectAttempts.getAndIncrement() > 0) {
			// Every connection gets a fresh attachment; the previous one was disposed
			this.websocket.abort();
			this.websocket.setAttachment(this.createWsData(this.ws));
		}

		final WebSocketCoreClient client;
		try {
			client = this.getClient();
		} catch (Exception e) {
			this.handleInternalError(e, WebsocketUtils.generateWsDataString(this.ws));
			return false;
		}

		final var responseHandshake = new AtomicReference<Handshake>(Handshake.EMPTY);
		var request = CoreClientUpgradeRequest.from(client, this.serverUri,
				this.createFrameHandler(responseHandshake));
		request.headers(headers -> this.httpHeaders.forEach(headers::put));
		request.addExtensions("permessage-deflate");
		request.addListener(new UpgradeListener() {
			@Override
			public void onHandshakeResponse(Request request, Response response) {
				responseHandshake.set(JettyWebSocket.toHandshake(response.getHeaders()));
			}
		});
		var configuration = new Configuration.ConfigurationCustomizer();
		JettyConfiguration.apply(configuration);
		request.setConfiguration(configuration);
		this.onWebsocketHandshakeSent(Handshake.of(this.httpHeaders));

		final CompletableFuture<?> future;
		try {
			future = client.connect(request);
		} catch (IOException e) {
			this.onConnectFailed(e);
			return false;
		}
		try {
			future.get(timeoutSeconds, TimeUnit.SECONDS);
			return true;
		} catch (TimeoutException e) {
			future.cancel(true);
			return false;
		} catch (ExecutionException e) {
			this.onConnectFailed(e.getCause());
			return false;
		}
	}

	private void onConnectFailed(Throwable cause) {
		if (cause instanceof UpgradeException ue && ue.getResponseStatusCode() > 0) {
			// Server rejected the handshake
			var reason = "Invalid status code received: " + ue.getResponseStatusCode();
			this.execute(new OnCloseHandler(//
					this.ws, CloseCode.PROTOCOL_ERROR, reason, true, //
					this.getOnClose(), //
					this::handleInternalError));
			this.logInfo(this.log, "Websocket [" + this.serverUri + "] closed." //
					+ " Code [" + CloseCode.PROTOCOL_ERROR + "]" //
					+ " Reason [" + reason + "]");
			this.reconnectorWorker.notifyHandshakeFailed(reason);
			return;
		}

		var root = cause instanceof UpgradeException && cause.getCause() != null ? cause.getCause() : cause;
		if (!(root instanceof ConnectException)) {
			// ConnectException is expected while the server is unavailable
			this.execute(new OnErrorHandler(//
					this.ws, root instanceof Exception e ? e : new Exception(root), //
					this.getOnError(), //
					this::handleInternalError));
		}
		this.execute(new OnCloseHandler(//
				this.ws, CloseCode.NEVER_CONNECTED, root.getMessage(), false, //
				this.getOnClose(), //
				this::handleInternalError));
	}

	private JettyFrameHandler createFrameHandler(AtomicReference<Handshake> responseHandshake) {
		return new JettyFrameHandler(this.websocket, new JettyFrameHandler.Listener() {

			@Override
			public void onOpen() {
				AbstractWebsocketClient.this.execute(new OnOpenHandler(//
						AbstractWebsocketClient.this.ws, responseHandshake.get(), //
						AbstractWebsocketClient.this.getOnOpen(), //
						AbstractWebsocketClient.this::logWarn, //
						AbstractWebsocketClient.this::handleInternalError));
				AbstractWebsocketClient.this.updateIsConnected();
			}

			@Override
			public void onText(String text) {
				AbstractWebsocketClient.this.execute(new OnMessageHandler(//
						AbstractWebsocketClient.this.ws, text, //
						AbstractWebsocketClient.this.getOnRequest(), //
						AbstractWebsocketClient.this.getOnNotification(), //
						AbstractWebsocketClient.this::sendMessage, //
						AbstractWebsocketClient.this::handleInternalError, //
						AbstractWebsocketClient.this::logWarn));
			}

			@Override
			public void onError(Exception cause) {
				AbstractWebsocketClient.this.execute(new OnErrorHandler(//
						AbstractWebsocketClient.this.ws, cause, //
						AbstractWebsocketClient.this.getOnError(), //
						AbstractWebsocketClient.this::handleInternalError));
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				AbstractWebsocketClient.this.execute(new OnCloseHandler(//
						AbstractWebsocketClient.this.ws, code, reason, remote, //
						AbstractWebsocketClient.this.getOnClose(), //
						AbstractWebsocketClient.this::handleInternalError));
				AbstractWebsocketClient.this.logInfo(AbstractWebsocketClient.this.log,
						"Websocket [" + AbstractWebsocketClient.this.serverUri + "] closed." //
								+ " Code [" + code + "]" //
								+ " Reason [" + reason + "]");
				AbstractWebsocketClient.this.updateIsConnected();
			}
		});
	}

	private void updateIsConnected() {
		var isOpen = this.ws.isOpen();
		if (this.isConnected.compareAndSet(!isOpen, isOpen)) {
			// Value has changed
			this.onConnectedChange.accept(isOpen);

			if (!isOpen) {
				this.reconnectorWorker.triggerNextRun();
			}
		}
	}

	/**
	 * Detects a lost connection, see
	 * {@link JettyWebSocket#checkConnectionLost(Duration)}.
	 */
	protected void checkConnectionLost() {
		this.websocket.checkConnectionLost(CONNECTION_LOST_TIMEOUT);
	}

	private synchronized WebSocketCoreClient getClient() throws Exception {
		if (this.client == null) {
			var threadPool = new QueuedThreadPool(MAX_JETTY_THREADS, 1);
			threadPool.setName(this.getName() + "-jetty");
			var httpClient = new HttpClient();
			httpClient.setExecutor(threadPool);
			var proxy = toJettyProxy(this.proxy);
			if (proxy != null) {
				httpClient.getProxyConfiguration().addProxy(proxy);
			}
			var client = new WebSocketCoreClient(httpClient, null);
			client.start();
			this.client = client;
		}
		return this.client;
	}

	private synchronized void stopClient() {
		var client = this.client;
		if (client == null) {
			return;
		}
		this.client = null;
		try {
			client.stop();
			client.getHttpClient().stop();
		} catch (Exception e) {
			this.logWarn(this.log, "Unable to stop websocket client. " + e.getClass().getSimpleName() + ": "
					+ e.getMessage());
		}
	}

	private static org.eclipse.jetty.client.ProxyConfiguration.Proxy toJettyProxy(Proxy proxy) {
		if (proxy == null || !(proxy.address() instanceof InetSocketAddress address)) {
			return null;
		}
		return switch (proxy.type()) {
		case DIRECT -> null;
		case HTTP -> new HttpProxy(address.getHostString(), address.getPort());
		case SOCKS -> new Socks5Proxy(address.getHostString(), address.getPort());
		};
	}

	/**
	 * Sends a {@link JsonrpcMessage} to the {@link WebSocket}. Returns true if
	 * sending was successful, otherwise false. Also logs a warning in that case.
	 *
	 * @param message the {@link JsonrpcMessage}
	 * @return true if sending was successful
	 */
	public boolean sendMessage(JsonrpcMessage message) {
		return this.sendMessage(this.ws, message);
	}

	@Override
	protected OnInternalError getOnInternalError() {
		return (t, wsDataString) -> {
			this.logError(this.log, new StringBuilder() //
					.append("OnInternalError for ").append(wsDataString).append(". ") //
					.append(t.getClass()).append(": ").append(t.getMessage()) //
					.toString());
			this.log.error(t.getMessage(), t);
		};
	}

	/**
	 * Sends a JSON-RPC Request and returns a future Response.
	 *
	 * @param request the JSON-RPC Request
	 * @return the future JSON-RPC Response
	 */
	public CompletableFuture<JsonrpcResponseSuccess> sendRequest(JsonrpcRequest request) {
		WsData wsData = this.ws.getAttachment();
		return wsData.send(request);
	}

	/**
	 * Gets some output that is suitable for a continuous Debug log.
	 *
	 * @return the debug log output or null
	 */
	public String debugLog() {
		if (this.ws.isOpen()) {
			return "Connected";
		} else {
			return "NOT CONNECTED. " + this.reconnectorWorker.debugLog();
		}
	}
}
