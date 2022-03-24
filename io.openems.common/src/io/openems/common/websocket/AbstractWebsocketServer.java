package io.openems.common.websocket;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
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
import io.openems.common.utils.ThreadPoolUtils;

public abstract class AbstractWebsocketServer<T extends WsData> extends AbstractWebsocket<T> {

	/**
	 * Shared {@link ExecutorService}. Configuration is equal to
	 * Executors.newCachedThreadPool(), but with DiscardOldestPolicy.
	 */
	protected final ScheduledThreadPoolExecutor executor;

	/*
	 * This Executor is used if Debug-Mode is activated.
	 */
	private final ScheduledExecutorService debugLogExecutor;

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketServer.class);
	private final int port;
	private final WebSocketServer ws;

	/**
	 * Construct an {@link AbstractWebsocketServer}.
	 *
	 * @param name      to identify this server
	 * @param port      to listen on
	 * @param poolSize  number of threads dedicated to handle the tasks
	 * @param debugMode activate a regular debug log about the state of the tasks
	 */
	protected AbstractWebsocketServer(String name, int port, int poolSize, boolean debugMode) {
		super(name);
		this.executor = new ScheduledThreadPoolExecutor(poolSize,
				new ThreadFactoryBuilder().setNameFormat(name + "-%d").build());

		// Debug-Mode
		if (debugMode) {
			this.debugLogExecutor = Executors.newSingleThreadScheduledExecutor();
			this.debugLogExecutor.scheduleWithFixedDelay(() -> {
				this.logInfo(this.log,
						String.format("[monitor] Pool: %d, Active: %d, Pending: %d, Completed: %d",
								this.executor.getPoolSize(), //
								this.executor.getActiveCount(), //
								this.executor.getQueue().size(), //
								this.executor.getCompletedTaskCount())); //
			}, 10, 10, TimeUnit.SECONDS);
		} else {
			this.debugLogExecutor = null;
		}

		this.port = port;
		this.ws = new WebSocketServer(new InetSocketAddress(port)) {

			@Override
			public void onStart() {
			}

			@Override
			public void onOpen(WebSocket ws, ClientHandshake handshake) {
				T wsData = AbstractWebsocketServer.this.createWsData();
				wsData.setWebsocket(ws);
				ws.setAttachment(wsData);
				var jHandshake = WebsocketUtils.handshakeToJsonObject(handshake);
				AbstractWebsocketServer.this.execute(new OnOpenHandler(AbstractWebsocketServer.this, ws, jHandshake));
			}

			@Override
			public void onMessage(WebSocket ws, String stringMessage) {
				try {
					JsonrpcMessage message;
					try {
						message = JsonrpcMessage.from(stringMessage);

					} catch (OpenemsNamedException e) {
						// handle deprecated non-JSON-RPC messages
						message = AbstractWebsocketServer.this.handleNonJsonrpcMessage(stringMessage, e);
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
				} catch (OpenemsNamedException e) {
					AbstractWebsocketServer.this.handleInternalErrorAsync(e);
				}
			}

			@Override
			public void onError(WebSocket ws, Exception ex) {
				if (ws == null) {
					AbstractWebsocketServer.this.handleInternalErrorAsync(ex);
				} else {
					AbstractWebsocketServer.this.execute(new OnErrorHandler(AbstractWebsocketServer.this, ws, ex));
				}
			}

			@Override
			public void onClose(WebSocket ws, int code, String reason, boolean remote) {
				AbstractWebsocketServer.this
						.execute(new OnCloseHandler(AbstractWebsocketServer.this, ws, code, reason, remote));
			}
		};
		// Allow the port to be reused. See
		// https://stackoverflow.com/questions/3229860/what-is-the-meaning-of-so-reuseaddr-setsockopt-option-linux
		this.ws.setReuseAddr(true);
	}

	@Override
	protected OnInternalError getOnInternalError() {
		return (ex, wsDataString) -> {
			if (ex instanceof BindException) {
				this.log.error("Unable to Bind to port [" + this.port + "]");
			} else {
				this.log.warn("OnInternalError for " + wsDataString + ". " + ex.getClass() + ": " + ex.getMessage());
			}
			ex.printStackTrace();
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
		ws.send(message.toString());
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
		this.log.info("Starting [" + this.getName() + "] websocket server [port=" + this.port + "]");
		this.ws.start();
	}

	/**
	 * Execute a {@link Runnable} using the shared {@link ExecutorService}.
	 *
	 * @param command the {@link Runnable}
	 */
	@Override
	protected void execute(Runnable command) {
		if (!this.executor.isShutdown()) {
			this.executor.execute(command);
		}
	}

	/**
	 * Stops the websocket server.
	 */
	@Override
	public void stop() {
		// Shutdown executors
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 5);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.debugLogExecutor, 5);

		var tries = 3;
		while (tries-- > 0) {
			try {
				this.ws.stop();
				return;
			} catch (NullPointerException | InterruptedException | IOException e) {
				this.log.warn("Unable to stop websocket server [" + this.getName() + "]. "
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					/* ignore */
				}
			}
		}
		this.log.error("Stopping websocket server [" + this.getName() + "] failed too often.");
		super.stop();
	}

	/**
	 * Handle Non-JSON-RPC messages.
	 *
	 * @param stringMessage the message
	 * @param e             the parse error
	 * @return message converted to {@link JsonrpcMessage}
	 * @throws OpenemsNamedException if conversion is not possible
	 */
	protected JsonrpcMessage handleNonJsonrpcMessage(String stringMessage, OpenemsNamedException e)
			throws OpenemsNamedException {
		throw new OpenemsException("Unhandled Non-JSON-RPC message", e);
	}

	/**
	 * Wraps the shared {@link ScheduledThreadPoolExecutor} of this
	 * {@link AbstractWebsocketServer}.
	 *
	 * @param command      see {@link ScheduledThreadPoolExecutor}
	 * @param initialDelay see {@link ScheduledThreadPoolExecutor}
	 * @param delay        see {@link ScheduledThreadPoolExecutor}
	 * @param unit         see {@link ScheduledThreadPoolExecutor}
	 * @return see {@link ScheduledThreadPoolExecutor}
	 */
	protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
			TimeUnit unit) {
		return this.executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

}
