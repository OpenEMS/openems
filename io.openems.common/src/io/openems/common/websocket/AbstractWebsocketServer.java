package io.openems.common.websocket;

import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;

import java.io.IOException;
import java.net.BindException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.core.FrameHandler;
import org.eclipse.jetty.websocket.core.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.core.server.ServerUpgradeResponse;
import org.eclipse.jetty.websocket.core.server.WebSocketNegotiator;
import org.eclipse.jetty.websocket.core.server.WebSocketUpgradeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.utils.ThreadPoolUtils;

public abstract class AbstractWebsocketServer<T extends WsData> extends AbstractWebsocket<T> {

	/**
	 * Interval for pings to detect lost connections.
	 */
	private static final Duration CONNECTION_LOST_TIMEOUT = Duration.ofSeconds(60);

	/**
	 * Allows large cookies, e.g. of OAuth sessions.
	 */
	private static final int MAX_REQUEST_HEADER_SIZE = 64 * 1024;

	/**
	 * Threads for Jetty I/O; message handling uses the {@link #executor}.
	 */
	private static final int MAX_JETTY_THREADS = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);

	/**
	 * Shared {@link ExecutorService}.
	 */
	private final ThreadPoolExecutor executor;

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketServer.class);
	private final int port;
	private final Collection<WebSocket> connections = ConcurrentHashMap.newKeySet();

	private Server server;
	private ServerConnector connector;
	private ScheduledExecutorService connectionLostChecker;
	private boolean isStarted = false;

	/**
	 * Construct an {@link AbstractWebsocketServer}.
	 *
	 * @param name     to identify this server
	 * @param port     to listen on; 0 to select a free port
	 * @param poolSize number of threads dedicated to handle the tasks
	 */
	protected AbstractWebsocketServer(String name, int port, int poolSize) {
		super(name);
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize,
				new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());
		this.port = port;
	}

	/**
	 * Returns a debug log of the current websocket state.
	 *
	 * @return the debug log string
	 */
	public String debugLog() {
		var b = new StringBuilder("[").append(this.getName()).append("] [monitor] ");
		if (this.isStarted) {
			b //
					.append("Connections: ").append(this.connections.size()).append(", ") //
					.append(ThreadPoolUtils.debugLog(this.executor));
		} else {
			b.append("NOT STARTED");
		}
		return b.toString();
	}

	/**
	 * Returns debug metrics of the current websocket state.
	 *
	 * @return the debug metrics
	 */
	public Map<String, Number> debugMetrics() {
		final var metrics = new HashMap<String, Number>();
		metrics.putAll(ThreadPoolUtils.debugMetrics(this.executor));
		metrics.put("Connections", this.connections.size());
		return metrics;
	}

	@Override
	protected OnInternalError getOnInternalError() {
		return (t, wsDataString) -> {
			switch (t) {
			case BindException be //
				-> this.logError(this.log, "Unable to Bind to port [" + this.port + "]");
			case IOException ioe when ioe.getCause() instanceof BindException //
				-> this.logError(this.log, "Unable to Bind to port [" + this.port + "]");
			default //
				-> this.logError(this.log, new StringBuilder() //
						.append("OnInternalError for ").append(wsDataString).append(". ") //
						.append(t.getClass()).append(": ") //
						.append(t.getMessage()).toString());
			}
			this.log.error(t.getMessage(), t);
		};
	}

	/**
	 * Handles the WebSocket handshake and creates the connection specific
	 * {@link WsData} attachment.
	 *
	 * <p>
	 * Override this method to validate the incoming handshake request and reject it
	 * by throwing a {@link HandshakeRejectedException}.
	 *
	 * @param ws        the current {@link WebSocket} connection
	 * @param handshake the incoming client handshake
	 * @return the {@link WsData} object that is attached to the WebSocket
	 * @throws HandshakeRejectedException if the handshake should be rejected
	 */
	protected T onHandshake(WebSocket ws, Handshake handshake) throws HandshakeRejectedException {
		return this.createWsData(ws);
	}

	public Collection<WebSocket> getConnections() {
		return this.connections;
	}

	/**
	 * Broadcasts a {@link JsonrpcMessage} to all connected WebSockets.
	 *
	 * @param message the {@link JsonrpcMessage}
	 */
	public void broadcastMessage(JsonrpcMessage message) {
		for (var ws : this.getConnections()) {
			this.sendMessage(ws, message);
		}
	}

	/**
	 * Broadcasts a {@link JsonrpcMessage} to all connected WebSockets matching a
	 * condition.
	 *
	 * @param message the {@link JsonrpcMessage}
	 * @param matcher the matching condition to send the message to
	 */
	public void broadcastMessage(JsonrpcMessage message, Predicate<T> matcher) {
		for (var ws : this.getConnections()) {
			T wsData = ws.getAttachment();
			if (!matcher.test(wsData)) {
				continue;
			}
			this.sendMessage(ws, message);
		}
	}

	/**
	 * Gets the port number that this server listens on.
	 *
	 * @return The port number.
	 */
	public int getPort() {
		var connector = this.connector;
		if (connector != null && connector.getLocalPort() > 0) {
			return connector.getLocalPort();
		}
		return this.port;
	}

	/**
	 * Starts the websocket server.
	 */
	@Override
	public synchronized void start() {
		if (this.isStarted) {
			return;
		}
		this.isStarted = true;
		super.start();
		this.logInfo(this.log, "Starting websocket server [port=" + this.port + "]");

		var threadPool = new QueuedThreadPool(MAX_JETTY_THREADS, 2);
		threadPool.setName(this.getName() + "-jetty");
		this.server = new Server(threadPool);

		var httpConfig = new HttpConfiguration();
		httpConfig.setRequestHeaderSize(MAX_REQUEST_HEADER_SIZE);
		this.connector = new ServerConnector(this.server, new HttpConnectionFactory(httpConfig));
		this.connector.setPort(this.port);
		this.connector.setReuseAddress(true);
		this.server.addConnector(this.connector);

		var upgradeHandler = new WebSocketUpgradeHandler(h -> h.addMapping("/*", WebSocketNegotiator.from(//
				(request, response, callback) -> this.createFrameHandler(request, response, callback), //
				(frameHandler, request, response) -> (FrameHandler) frameHandler)));
		JettyConfiguration.apply(upgradeHandler.getConfiguration());
		this.server.setHandler(upgradeHandler);

		try {
			this.server.start();
		} catch (Exception e) {
			this.handleInternalError(e, "");
			this.stopJettyServer();
			return;
		}

		this.connectionLostChecker = Executors.newSingleThreadScheduledExecutor(
				new ThreadFactoryBuilder().setNameFormat(this.getName() + "-ConnectionLost").setDaemon(true).build());
		this.connectionLostChecker.scheduleWithFixedDelay(() -> {
			for (var ws : this.connections) {
				((JettyWebSocket) ws).checkConnectionLost(CONNECTION_LOST_TIMEOUT);
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	private JettyFrameHandler createFrameHandler(ServerUpgradeRequest request, ServerUpgradeResponse response,
			Callback callback) {
		var handshake = JettyWebSocket.toHandshake(request.getHeaders());
		var ws = new JettyWebSocket(request.getConnectionMetaData().getRemoteSocketAddress());
		try {
			ws.setAttachment(this.onHandshake(ws, handshake));
		} catch (HandshakeRejectedException e) {
			Response.writeError(request, response, callback, HttpStatus.FORBIDDEN_403, e.getMessage());
			return null;
		}
		// Servers do not support permessage-deflate
		response.setExtensions(List.of());
		return new JettyFrameHandler(ws, new JettyFrameHandler.Listener() {

			@Override
			public void onOpen() {
				AbstractWebsocketServer.this.connections.add(ws);
				AbstractWebsocketServer.this.execute(new OnOpenHandler(//
						ws, handshake, //
						AbstractWebsocketServer.this.getOnOpen(), //
						AbstractWebsocketServer.this::logWarn, //
						AbstractWebsocketServer.this::handleInternalError));
			}

			@Override
			public void onText(String text) {
				AbstractWebsocketServer.this.execute(new OnMessageHandler(//
						ws, text, //
						AbstractWebsocketServer.this.getOnRequest(), //
						AbstractWebsocketServer.this.getOnNotification(), //
						AbstractWebsocketServer.this::sendMessage, //
						AbstractWebsocketServer.this::handleInternalError, //
						AbstractWebsocketServer.this::logWarn));
			}

			@Override
			public void onError(Exception cause) {
				AbstractWebsocketServer.this.execute(new OnErrorHandler(//
						ws, cause, //
						AbstractWebsocketServer.this.getOnError(), //
						AbstractWebsocketServer.this::handleInternalError));
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				AbstractWebsocketServer.this.connections.remove(ws);
				AbstractWebsocketServer.this.execute(new OnCloseHandler(//
						ws, code, reason, remote, //
						AbstractWebsocketServer.this.getOnClose(), //
						AbstractWebsocketServer.this::handleInternalError));
			}
		});
	}

	/**
	 * Execute a {@link Runnable} using the shared {@link ExecutorService}.
	 *
	 * @param command the {@link Runnable}
	 */
	@Override
	protected void execute(Runnable command) {
		if (this.executor.isShutdown()) {
			// Avoid rejectedExecution during shutdown
			command.run();
		} else {
			this.executor.execute(command);
		}
	}

	/**
	 * Stops the websocket server.
	 */
	@Override
	public synchronized void stop() {
		if (!this.isStarted) {
			return;
		}
		this.isStarted = false;

		if (this.connectionLostChecker != null) {
			this.connectionLostChecker.shutdownNow();
			this.connectionLostChecker = null;
		}

		// Shutdown executors
		shutdownAndAwaitTermination(this.executor, 5);

		this.stopJettyServer();
		super.stop();
	}

	private void stopJettyServer() {
		var server = this.server;
		if (server == null) {
			return;
		}
		try {
			server.stop();
		} catch (Exception e) {
			this.logWarn(this.log,
					"Unable to stop websocket server. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		this.server = null;
		this.connector = null;
		this.connections.clear();
	}
}
