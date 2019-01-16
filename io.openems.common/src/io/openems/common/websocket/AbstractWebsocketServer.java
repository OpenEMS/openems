package io.openems.common.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

public abstract class AbstractWebsocketServer<T extends WsData> extends AbstractWebsocket<T> {

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketServer.class);
	private final int port;
	private final WebSocketServer ws;

	/**
	 * @param name to identify this server
	 * @param port to listen on
	 */
	protected AbstractWebsocketServer(String name, int port) {
		super(name);
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
				JsonObject jHandshake = WebsocketUtils.handshakeToJsonObject(handshake);
				CompletableFuture.runAsync(new OnOpenHandler(AbstractWebsocketServer.this, ws, jHandshake));
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
						CompletableFuture.runAsync(new OnRequestHandler(AbstractWebsocketServer.this, ws,
								(JsonrpcRequest) message, (response) -> {
									AbstractWebsocketServer.this.sendMessage(ws, response);
								}));

					} else if (message instanceof JsonrpcResponse) {
						CompletableFuture.runAsync(
								new OnResponseHandler(AbstractWebsocketServer.this, ws, (JsonrpcResponse) message));

					} else if (message instanceof JsonrpcNotification) {
						CompletableFuture.runAsync(new OnNotificationHandler(AbstractWebsocketServer.this, ws,
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
					CompletableFuture.runAsync(new OnErrorHandler(AbstractWebsocketServer.this, ws, ex));
				}
			}

			@Override
			public void onClose(WebSocket ws, int code, String reason, boolean remote) {
				CompletableFuture.runAsync(new OnCloseHandler(AbstractWebsocketServer.this, ws, code, reason, remote));
			}
		};
	}

	public Collection<WebSocket> getConnections() {
		return this.ws.getConnections();
	}

	public void sendMessage(WebSocket ws, JsonrpcMessage message) {
		ws.send(message.toString());
	}

	/**
	 * Starts the websocket server
	 */
	public void start() {
		log.info("Starting [" + this.getName() + "] websocket server [port=" + this.port + "]");
		this.ws.start();
	}

	/**
	 * Stops the websocket server
	 */
	public void stop() {
		int tries = 3;
		while (tries-- > 0) {
			try {
				this.ws.stop();
				return;
			} catch (NullPointerException | InterruptedException | IOException e) {
				log.warn("Unable to stop websocket server [" + this.getName() + "]. " + e.getClass().getSimpleName()
						+ ": " + e.getMessage());
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					/* ignore */
				}
			}
		}
		log.error("Stopping websocket server [" + this.getName() + "] failed too often.");
	}

	/**
	 * Convert deprecated Non-JSON-RPC messages to JSON-RPC messages.
	 * 
	 * @param stringMessage
	 * @return
	 */
	protected JsonrpcMessage handleNonJsonrpcMessage(String stringMessage, OpenemsNamedException e)
			throws OpenemsNamedException {
		throw new OpenemsException("Unhandled Non-JSON-RPC message", e);
	}

}
