package io.openems.common.websocket;

import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.utils.ThreadPoolUtils;

public abstract class AbstractWebsocketServer<T extends WsData> extends AbstractWebsocket<T> {

	/**
	 * Shared {@link ExecutorService}.
	 */
	private final ThreadPoolExecutor executor;

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketServer.class);
	private final int port;
	private final WebSocketServer ws;
	private final Collection<WebSocket> connections = ConcurrentHashMap.newKeySet();

	/**
	 * Construct an {@link AbstractWebsocketServer}.
	 *
	 * @param name     to identify this server
	 * @param port     to listen on
	 * @param poolSize number of threads dedicated to handle the tasks
	 */
	protected AbstractWebsocketServer(String name, int port, int poolSize) {
		super(name);
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize,
				new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());

		this.port = port;
		this.ws = new WebSocketServer(new InetSocketAddress(port),
				/* AVAILABLE_PROCESSORS */ Runtime.getRuntime().availableProcessors(), //
				/* drafts, no filter */ List.of(new MyDraft6455()), //
				this.connections) {

			@Override
			public void onStart() {
			}

			@Override
			public void onOpen(WebSocket ws, ClientHandshake handshake) {
				T wsData = AbstractWebsocketServer.this.createWsData(ws);
				ws.setAttachment(wsData);
				AbstractWebsocketServer.this.execute(new OnOpenHandler(//
						ws, handshake, //
						AbstractWebsocketServer.this.getOnOpen(), //
						AbstractWebsocketServer.this::logWarn, //
						AbstractWebsocketServer.this::handleInternalError));
			}

			@Override
			public void onMessage(WebSocket ws, String message) {
				AbstractWebsocketServer.this.execute(new OnMessageHandler(//
						ws, message, //
						AbstractWebsocketServer.this.getOnRequest(), //
						AbstractWebsocketServer.this.getOnNotification(), //
						AbstractWebsocketServer.this::sendMessage, //
						AbstractWebsocketServer.this::handleInternalError, //
						AbstractWebsocketServer.this::logWarn));
			}

			@Override
			public void onError(WebSocket ws, Exception ex) {
				AbstractWebsocketServer.this.execute(new OnErrorHandler(//
						ws, ex, //
						AbstractWebsocketServer.this.getOnError(), //
						AbstractWebsocketServer.this::handleInternalError));
			}

			@Override
			public void onClose(WebSocket ws, int code, String reason, boolean remote) {
				AbstractWebsocketServer.this.execute(new OnCloseHandler(//
						ws, code, reason, remote, //
						AbstractWebsocketServer.this.getOnClose(), //
						AbstractWebsocketServer.this::handleInternalError));
			}

			@Override
			protected boolean removeConnection(WebSocket ws) {
				return AbstractWebsocketServer.this.connections.remove(ws);

				// TODO overridden method also does:
				// if (isclosed.get() && connections.isEmpty()) {
				// selectorthread.interrupt();
				// }
			}

			@Override
			protected boolean addConnection(WebSocket ws) {
				return AbstractWebsocketServer.this.connections.add(ws);

				// TODO overridden method also does:
				// if (!isclosed.get()) {
				// synchronized (connections) {
				// return this.connections.add(ws);
				// }
				// } else {
				// // This case will happen when a new connection gets ready while the server is
				// // already stopping.
				// ws.close(CloseFrame.GOING_AWAY);
				// return true;// for consistency sake we will make sure that both onOpen will
				// be called
				// }
			}

			@Override
			public Collection<WebSocket> getConnections() {
				return AbstractWebsocketServer.this.connections;
			}
		};

		// Allow the port to be reused. See
		// https://stackoverflow.com/questions/3229860/what-is-the-meaning-of-so-reuseaddr-setsockopt-option-linux
		this.ws.setReuseAddr(true);
	}

	/**
	 * Returns a debug log of the current websocket state.
	 * 
	 * @return the debug log string
	 */
	public String debugLog() {
		return new StringBuilder("[monitor] ") //
				.append("Connections: ").append(this.connections.size()).append(", ") //
				.append(ThreadPoolUtils.debugLog(this.executor)) //
				.toString();
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
			if (t instanceof BindException) {
				this.logError(this.log, "Unable to Bind to port [" + this.port + "]");
			} else {
				this.logError(this.log, new StringBuilder() //
						.append("OnInternalError for ").append(wsDataString).append(". ") //
						.append(t.getClass()).append(": ") //
						.append(t.getMessage()).toString());
			}
			t.printStackTrace();
		};
	}

	public Collection<WebSocket> getConnections() {
		return this.ws.getConnections();
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
	 * Gets the port number that this server listens on.
	 *
	 * @return The port number.
	 */
	public int getPort() {
		return this.ws.getPort();
	}

	/**
	 * Starts the {@link WebSocketServer}.
	 */
	@Override
	public void start() {
		super.start();
		this.logInfo(this.log, "Starting websocket server [port=" + this.port + "]");
		this.ws.start();
	}

	/**
	 * Execute a {@link Runnable} using the shared {@link ExecutorService}.
	 *
	 * @param command the {@link Runnable}
	 */
	@Override
	protected void execute(Runnable command) {
		this.executor.execute(command);
	}

	/**
	 * Stops the {@link WebSocketServer}.
	 */
	@Override
	public void stop() {
		// Shutdown executors
		shutdownAndAwaitTermination(this.executor, 5);

		var tries = 3;
		while (tries-- > 0) {
			try {
				this.ws.stop();
				return;
			} catch (NullPointerException | InterruptedException e) {
				this.logWarn(this.log,
						"Unable to stop websocket server. " + e.getClass().getSimpleName() + ": " + e.getMessage());
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					/* ignore */
				}
			}
		}
		this.logError(this.log, "Stopping websocket server failed too often.");
		super.stop();
	}
}
