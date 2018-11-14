package io.openems.common.websocket;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

public abstract class AbstractWebsocketServer {

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketServer.class);
	private final String name;
	private final int port;
	private final WebSocketServer ws;

	protected abstract WsData onOpen(WebSocket ws, JsonObject handshake) throws OpenemsException;

	protected abstract void onRequest(WebSocket ws, JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback)
			throws OpenemsException;

	protected abstract void onError(WebSocket ws, Exception ex) throws OpenemsException;

	protected abstract void onClose(WebSocket ws, int code, String reason, boolean remote) throws OpenemsException;

	protected abstract void onInternalError(Exception ex);

	protected AbstractWebsocketServer(String name, int port) {
		this.name = name;
		this.port = port;
		this.ws = new WebSocketServer(new InetSocketAddress(port)) {

			@Override
			public void onStart() {
			}

			@Override
			public void onOpen(WebSocket ws, ClientHandshake handshake) {
				WsData wsData = null;
				try {
					JsonObject jHandshake = WebsocketUtils.handshakeToJsonObject(handshake);
					wsData = AbstractWebsocketServer.this.onOpen(ws, jHandshake);
				} catch (OpenemsException e) {
					AbstractWebsocketServer.this.onInternalError(e);
				}
				ws.setAttachment(wsData);
			}

			@Override
			public void onMessage(WebSocket ws, String stringMessage) {
				try {
					JsonrpcMessage message = JsonrpcMessage.from(stringMessage);
					if (message instanceof JsonrpcRequest) {
						AbstractWebsocketServer.this.onRequest(ws, (JsonrpcRequest) message, (response) -> {
							if (response != null) {
								AbstractWebsocketServer.this.sendMessage(ws, response);
							}
						});

					} else if (message instanceof JsonrpcResponse) {
						WsData wsData = ws.getAttachment();
						wsData.handleJsonrpcResponse((JsonrpcResponse) message);
					}
				} catch (OpenemsException e) {
					AbstractWebsocketServer.this.onInternalError(e);
				}
			}

			@Override
			public void onError(WebSocket ws, Exception ex) {
				try {
					AbstractWebsocketServer.this.onError(ws, ex);
				} catch (OpenemsException e) {
					AbstractWebsocketServer.this.onInternalError(e);
				}
			}

			@Override
			public void onClose(WebSocket ws, int code, String reason, boolean remote) {
				try {
					AbstractWebsocketServer.this.onClose(ws, code, reason, remote);
				} catch (OpenemsException e) {
					AbstractWebsocketServer.this.onInternalError(e);
				}
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
		log.info("Starting [" + this.name + "] websocket server [port=" + this.port + "]");
		this.ws.start();
	}

	/**
	 * Stops the websocket server
	 */
	public void stop() {
		int tries = 3;
		while (tries-- > 0) {
			try {
				this.ws.stop(1000);
				return;
			} catch (NullPointerException | InterruptedException e) {
				log.warn("Unable to stop websocket server [" + this.name + "]. " + e.getClass().getSimpleName() + ": "
						+ e.getMessage());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					/* ignore */
				}
			}
		}
		log.error("Stopping websocket server [" + this.name + "] failed too often.");
	}

}
