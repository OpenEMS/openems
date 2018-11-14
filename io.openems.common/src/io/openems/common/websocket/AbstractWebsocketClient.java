package io.openems.common.websocket;

import java.net.URI;
import java.util.function.Consumer;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;

public abstract class AbstractWebsocketClient {

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketServer.class);
	private final WebSocketClient ws;
	private final URI serverUri;

	private volatile WsData wsData = null;

	protected abstract WsData onOpen(JsonObject handshake);

	protected abstract void onRequest(JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback)
			throws OpenemsException;

	protected abstract void onError(Exception ex) throws OpenemsException;

	protected abstract void onClose(int code, String reason, boolean remote) throws OpenemsException;

	protected abstract void onInternalError(Exception ex);

	protected AbstractWebsocketClient(URI serverUri) {
		this.serverUri = serverUri;
		this.ws = new WebSocketClient(serverUri) {

			@Override
			public void onOpen(ServerHandshake handshake) {
				JsonObject jHandshake = WebsocketUtils.handshakeToJsonObject(handshake);
				WsData wsData = AbstractWebsocketClient.this.onOpen(jHandshake);
				AbstractWebsocketClient.this.wsData = wsData;
			}

			@Override
			public void onMessage(String stringMessage) {
				try {
					JsonrpcMessage message = JsonrpcMessage.from(stringMessage);
					if (message instanceof JsonrpcRequest) {
						AbstractWebsocketClient.this.onRequest((JsonrpcRequest) message, (response) -> {
							if (response != null) {
								AbstractWebsocketClient.this.sendMessage(response);
							}
						});

					} else if (message instanceof JsonrpcResponse) {
						AbstractWebsocketClient.this.wsData.handleJsonrpcResponse((JsonrpcResponse) message);
					}
				} catch (OpenemsException e) {
					AbstractWebsocketClient.this.onInternalError(e);
				}
			}

			@Override
			public void onError(Exception ex) {
				try {
					AbstractWebsocketClient.this.onError(ex);
				} catch (OpenemsException e) {
					AbstractWebsocketClient.this.onInternalError(e);
				}
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				try {
					AbstractWebsocketClient.this.onClose(code, reason, remote);
				} catch (OpenemsException e) {
					AbstractWebsocketClient.this.onInternalError(e);
				}
			}
		};
	}

	/**
	 * Starts the websocket server
	 * 
	 * @throws InterruptedException
	 */
	public void connectBlocking() throws InterruptedException {
		log.info("Connecting to websocket server [" + this.serverUri + "]");
		this.ws.connectBlocking();
	}

	/**
	 * Stops the websocket server
	 */
	public void disconnect() {
		log.info("Closed connection to websocket server [" + this.serverUri + "]");
		this.ws.close(0, "Application exited");
	}

	public void sendMessage(JsonrpcMessage message) {
		ws.send(message.toString());
	}

	public void sendRequest(JsonrpcRequest request, Consumer<JsonrpcResponse> responseCallback)
			throws OpenemsException {
		this.wsData.send(ws, request, responseCallback);
	}

}
