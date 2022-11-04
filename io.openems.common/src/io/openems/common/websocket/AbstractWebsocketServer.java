package io.openems.common.websocket;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
	 * Shared {@link ExecutorService}.
	 */
	protected final ThreadPoolExecutor executor;

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
	 * @param name          to identify this server
	 * @param port          to listen on
	 * @param poolSize      number of threads dedicated to handle the tasks
	 * @param debugMode     activate a regular debug log about the state of the
	 *                      tasks
	 * @param debugCallback additional callback on regular debug log
	 */
	protected AbstractWebsocketServer(String name, int port, int poolSize, boolean debugMode,
			Consumer<ThreadPoolExecutor> debugCallback) {
		super(name);
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize,
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
				if (debugCallback != null) {
					debugCallback.accept(this.executor);
				}

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
						try {
							message = JsonrpcMessage.from(stringMessage);

						} catch (OpenemsNamedException e) {
							// handle deprecated non-JSON-RPC messages
							message = AbstractWebsocketServer.this.handleNonJsonrpcMessage(ws, stringMessage, e);
						}
					} catch (OpenemsNamedException e) {
						AbstractWebsocketServer.this.handleInternalErrorAsync(e, WebsocketUtils.getWsDataString(ws));
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
					if (ws == null) {
						AbstractWebsocketServer.this.handleInternalErrorAsync(ex, WebsocketUtils.getWsDataString(ws));
					} else {
						AbstractWebsocketServer.this.execute(new OnErrorHandler(AbstractWebsocketServer.this, ws, ex));
					}

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
		};
		// Allow the port to be reused. See
		// https://stackoverflow.com/questions/3229860/what-is-the-meaning-of-so-reuseaddr-setsockopt-option-linux
		this.ws.setReuseAddr(true);
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
		this.logInfo(this.log, "Starting websocket server [port=" + this.port + "]");
		this.ws.start();
	}

	/**
	 * Execute a {@link Runnable} using the shared {@link ExecutorService}.
	 *
	 * @param command the {@link Runnable}
	 */
	@Override
	protected void execute(Runnable command) throws RejectedExecutionException {
		this.executor.execute(command);
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
	 * @return message converted to {@link JsonrpcMessage}
	 * @throws OpenemsNamedException if conversion is not possible
	 */
	protected JsonrpcMessage handleNonJsonrpcMessage(WebSocket ws, String stringMessage, OpenemsNamedException e)
			throws OpenemsNamedException {
		throw new OpenemsException("Unhandled Non-JSON-RPC message", e);
	}

}
