package io.openems.common.websocket;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.utils.JsonrpcUtils;
import io.openems.common.utils.StringUtils;
import io.openems.common.utils.ThreadPoolUtils;

public abstract class AbstractWebsocketServer<T extends WsData> extends AbstractWebsocket<T> {
	private boolean stopping = false;

	public static enum DebugMode {
		OFF, SIMPLE, DETAILED;

		/**
		 * Is this {@link DebugMode} at least as high as the other {@link DebugMode}?.
		 * 
		 * @param other the other {@link DebugMode}
		 * @return true if yes
		 */
		public boolean isAtLeast(DebugMode other) {
			return this.ordinal() >= other.ordinal();
		}
	}

	/**
	 * Shared {@link ExecutorService}.
	 */
	private final ThreadPoolExecutor executor;

	private final ConcurrentHashMap<String, AtomicInteger> activeTasks = new ConcurrentHashMap<>(100);
	private static final Function<String, AtomicInteger> ATOMIC_INTEGER_PROVIDER = (key) -> {
		return new AtomicInteger(0);
	};

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketServer.class);
	private final int port;
	private final WebSocketServer ws;
	private final DebugMode debugMode;
	private final Collection<WebSocket> connections = ConcurrentHashMap.newKeySet();
	private final Draft perMessageDeflateDraft = new Draft_6455(new PerMessageDeflateExtension());

	/**
	 * Construct an {@link AbstractWebsocketServer}.
	 *
	 * @param name      to identify this server
	 * @param port      to listen on
	 * @param poolSize  number of threads dedicated to handle the tasks
	 * @param debugMode activate a regular debug log about the state of the tasks
	 */
	protected AbstractWebsocketServer(String name, int port, int poolSize, DebugMode debugMode) {
		super(name);
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize,
				new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());

		this.port = port;
		this.ws = new WebSocketServer(new InetSocketAddress(port),
				/* AVAILABLE_PROCESSORS */ Runtime.getRuntime().availableProcessors(), //
				/* enable perMessageDeflate */ Collections.singletonList(this.perMessageDeflateDraft), //
				this.connections) {

			@Override
			public void onStart() {
			}

			@Override
			public void onOpen(WebSocket ws, ClientHandshake handshake) {
				try {
					T wsData = AbstractWebsocketServer.this.createWsData();
					wsData.setWebsocket(ws);
					ws.setAttachment(wsData);
					var jHandshake = WebsocketUtils.handshakeToJsonObject(handshake);
					AbstractWebsocketServer.this
							.execute(new OnOpenHandler(AbstractWebsocketServer.this, ws, jHandshake));

				} catch (Throwable t) {
					AbstractWebsocketServer.this.handleInternalErrorSync(t, WebsocketUtils.getWsDataString(ws));
				}
			}

			@Override
			public void onMessage(WebSocket ws, String stringMessage) {
				try {
					JsonrpcMessage message;
					try {
						message = JsonrpcMessage.from(stringMessage);
					} catch (OpenemsNamedException e) {
						// handle deprecated non-JSON-RPC messages
						message = AbstractWebsocketServer.this.handleNonJsonrpcMessage(ws, stringMessage, e);
					}
					if (message == null) {
						// silently ignore 'null'
						return;
					}

					if (message instanceof JsonrpcRequest) {
						AbstractWebsocketServer.this.execute(new OnRequestHandler(AbstractWebsocketServer.this, ws,
								(JsonrpcRequest) message, response -> {
									AbstractWebsocketServer.this.sendMessage(ws, response);
								}));

					} else if (message instanceof JsonrpcResponse) {
						AbstractWebsocketServer.this.execute(
								new OnResponseHandler(AbstractWebsocketServer.this, ws, (JsonrpcResponse) message));

					} else if (message instanceof JsonrpcNotification) {
						AbstractWebsocketServer.this.execute(new OnNotificationHandler(AbstractWebsocketServer.this, ws,
								(JsonrpcNotification) message));
					}

				} catch (Throwable t) {
					AbstractWebsocketServer.this.handleInternalErrorSync(t, WebsocketUtils.getWsDataString(ws));
				}
			}

			@Override
			public void onError(WebSocket ws, Exception ex) {
				try {
					AbstractWebsocketServer.this.execute(new OnErrorHandler(AbstractWebsocketServer.this, ws, ex));
				} catch (Throwable t) {
					AbstractWebsocketServer.this.handleInternalErrorSync(t, WebsocketUtils.getWsDataString(ws));
				}
			}

			@Override
			public void onClose(WebSocket ws, int code, String reason, boolean remote) {
				try {
					AbstractWebsocketServer.this
							.execute(new OnCloseHandler(AbstractWebsocketServer.this, ws, code, reason, remote));

				} catch (Throwable t) {
					AbstractWebsocketServer.this.handleInternalErrorSync(t, WebsocketUtils.getWsDataString(ws));
				}
			}

			@Override
			protected synchronized boolean removeConnection(WebSocket ws) {
				return AbstractWebsocketServer.this.connections.remove(ws);
			}

			@Override
			protected synchronized boolean addConnection(WebSocket ws) {
				if (!AbstractWebsocketServer.this.stopping) {
					return AbstractWebsocketServer.this.connections.add(ws);
				} else {
					ws.close(CloseFrame.GOING_AWAY);
					return true; // Ensure onOpen will be called for consistency
				}
			}

			@Override
			public Collection<WebSocket> getConnections() {
				return AbstractWebsocketServer.this.connections;
			}
		};
		// Allow the port to be reused. See
		// https://stackoverflow.com/questions/3229860/what-is-the-meaning-of-so-reuseaddr-setsockopt-option-linux
		this.ws.setReuseAddr(true);

		// Debug-Mode
		this.debugMode = debugMode == null ? DebugMode.OFF : debugMode;
	}

	/**
	 * Stops the server.
	 */
	public synchronized void stopServer() {
		AbstractWebsocketServer.this.stopping = true;
		this.stop();
	}

	/**
	 * Returns a debug log of the current websocket state.
	 * 
	 * @return the debug log string
	 */
	public String debugLog() {
		var b = new StringBuilder("[monitor] ") //
				.append("Connections: ").append(this.ws.getConnections().size()).append(", ") //
				.append(ThreadPoolUtils.debugLog(this.executor)); //
		if (this.debugMode.isAtLeast(DebugMode.DETAILED) && this.executor.getActiveCount() > 0) {
			b.append(", Tasks: ");
			this.activeTasks.forEach((id, count) -> {
				var cnt = count.get();
				if (cnt > 0) {
					b.append(id).append(':').append(cnt).append(", ");
				}
			});
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
			if (t instanceof BindException) {
				this.logError(this.log, "Unable to Bind to port [" + this.port + "]");
			} else {
				this.logError(this.log,
						"OnInternalError for " + wsDataString + ". " + t.getClass() + ": " + t.getMessage());
			}
			t.printStackTrace();
		};
	}

	public Collection<WebSocket> getConnections() {
		return this.ws.getConnections();
	}

	/**
	 * Sends a message to WebSocket.
	 *
	 * @param ws      the WebSocket
	 * @param message the JSON-RPC Message
	 */
	public void sendMessage(WebSocket ws, JsonrpcMessage message) {
		try {
			ws.send(message.toString());

		} catch (WebsocketNotConnectedException e) {
			WsData wsData = ws.getAttachment();

			if (wsData != null) {
				this.logWarn(this.log, "Connection [" + wsData.toString() + "] is closed. Unable to send message: "
						+ StringUtils.toShortString(JsonrpcUtils.simplifyJsonrpcMessage(message), 100));
			} else {
				this.logWarn(this.log, "Connection is closed. Unable to send message: "
						+ StringUtils.toShortString(JsonrpcUtils.simplifyJsonrpcMessage(message), 100));
			}
		}
	}

	/**
	 * Broadcasts a message to all connected WebSockets.
	 *
	 * @param message the JSON-RPC Message
	 */
	public void broadcastMessage(JsonrpcMessage message) {
		for (WebSocket ws : this.getConnections()) {
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
	 * Starts the websocket server.
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
		if (this.debugMode.isAtLeast(DebugMode.DETAILED)) {
			this.executor.execute(() -> {
				String id = AbstractWebsocketServer.getRunnableIdentifier(command);
				try {
					this.activeTasks.computeIfAbsent(id, ATOMIC_INTEGER_PROVIDER).incrementAndGet();
					command.run();
				} catch (Throwable t) {
					throw t;
				} finally {
					this.activeTasks.get(id).decrementAndGet();
				}
			});
		} else {
			this.executor.execute(command);
		}
	}

	private static final String getRunnableIdentifier(Runnable r) {
		if (r instanceof OnRequestHandler) {
			return ((OnRequestHandler) r).getRequestMethod();
		}
		return r.getClass().getSimpleName();
	}

	/**
	 * Stops the websocket server.
	 */
	@Override
	public void stop() {
		// Shutdown executors
		this.stopping = true;

		for (WebSocket ws : this.connections) {
			ws.close(CloseFrame.GOING_AWAY);
		}

		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);

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

	/**
	 * Handle Non-JSON-RPC messages.
	 * 
	 * @param ws            the {@link WebSocket}
	 * @param stringMessage the message
	 * @param e             the parse error
	 * @return message converted to {@link JsonrpcMessage}; or null
	 * @throws OpenemsNamedException if conversion is not possible
	 */
	protected JsonrpcMessage handleNonJsonrpcMessage(WebSocket ws, String stringMessage, OpenemsNamedException e)
			throws OpenemsNamedException {
		throw new OpenemsException("Unhandled Non-JSON-RPC message", e);
	}

}
