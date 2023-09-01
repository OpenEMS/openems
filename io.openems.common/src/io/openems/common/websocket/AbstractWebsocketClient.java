package io.openems.common.websocket;

import java.net.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponse;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.StringUtils;

/**
 * A Websocket Client implementation that automatically tries to reconnect a
 * closed connection.
 *
 * @param <T> the type of websocket attachments inheriting {@link WsData}
 */
public abstract class AbstractWebsocketClient<T extends WsData> extends AbstractWebsocket<T> {

	public static final Map<String, String> NO_HTTP_HEADERS = new HashMap<>();
	public static final Proxy NO_PROXY = null;
	public static final Draft DEFAULT_DRAFT = new Draft_6455();

	protected final WebSocketClient ws;

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocketClient.class);
	private final URI serverUri;
	private final ClientReconnectorWorker reconnectorWorker;

	protected AbstractWebsocketClient(String name, URI serverUri) {
		this(name, serverUri, AbstractWebsocketClient.DEFAULT_DRAFT, AbstractWebsocketClient.NO_HTTP_HEADERS,
				AbstractWebsocketClient.NO_PROXY);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders) {
		this(name, serverUri, AbstractWebsocketClient.DEFAULT_DRAFT, httpHeaders, AbstractWebsocketClient.NO_PROXY);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Map<String, String> httpHeaders, Proxy proxy) {
		this(name, serverUri, AbstractWebsocketClient.DEFAULT_DRAFT, httpHeaders, proxy);
	}

	protected AbstractWebsocketClient(String name, URI serverUri, Draft draft, Map<String, String> httpHeaders,
			Proxy proxy) {
		super(name);
		this.serverUri = serverUri;
		this.ws = new WebSocketClient(serverUri, draft, httpHeaders) {

			private void logInfo(String message) {
				AbstractWebsocketClient.this.logInfo(AbstractWebsocketClient.this.log, message);
			}

			@Override
			public void onOpen(ServerHandshake handshake) {
				try {
					var jHandshake = WebsocketUtils.handshakeToJsonObject(handshake);
					AbstractWebsocketClient.this.execute(new OnOpenHandler(AbstractWebsocketClient.this,
							AbstractWebsocketClient.this.ws, jHandshake));

				} catch (Exception e) {
					AbstractWebsocketClient.this.handleInternalErrorSync(e,
							WebsocketUtils.getWsDataString(AbstractWebsocketClient.this.ws));
				}
			}

			@Override
			public void onMessage(String stringMessage) {
				final JsonrpcMessage message;
				try {
					message = JsonrpcMessage.from(stringMessage);
				} catch (OpenemsNamedException e) {
					AbstractWebsocketClient.this.handleInternalErrorAsync(e,
							WebsocketUtils.getWsDataString(AbstractWebsocketClient.this.ws));
					return;
				}

				try {
					if (message instanceof JsonrpcRequest) {
						AbstractWebsocketClient.this.execute(new OnRequestHandler(AbstractWebsocketClient.this,
								AbstractWebsocketClient.this.ws, (JsonrpcRequest) message, response -> {
									AbstractWebsocketClient.this.sendMessage(response);
								}));

					} else if (message instanceof JsonrpcResponse) {
						AbstractWebsocketClient.this.execute(new OnResponseHandler(AbstractWebsocketClient.this,
								AbstractWebsocketClient.this.ws, (JsonrpcResponse) message));

					} else if (message instanceof JsonrpcNotification) {
						AbstractWebsocketClient.this.execute(new OnNotificationHandler(AbstractWebsocketClient.this,
								AbstractWebsocketClient.this.ws, (JsonrpcNotification) message));
					}

				} catch (Exception e) {
					AbstractWebsocketClient.this.handleInternalErrorSync(e,
							WebsocketUtils.getWsDataString(AbstractWebsocketClient.this.ws));
				}
			}

			@Override
			public void onError(Exception ex) {
				try {
					AbstractWebsocketClient.this.execute(
							new OnErrorHandler(AbstractWebsocketClient.this, AbstractWebsocketClient.this.ws, ex));

				} catch (Exception e) {
					AbstractWebsocketClient.this.handleInternalErrorSync(e,
							WebsocketUtils.getWsDataString(AbstractWebsocketClient.this.ws));
				}
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				try {
					AbstractWebsocketClient.this.execute(new OnCloseHandler(AbstractWebsocketClient.this,
							AbstractWebsocketClient.this.ws, code, reason, remote));

				} catch (Exception e) {
					AbstractWebsocketClient.this.handleInternalErrorSync(e,
							WebsocketUtils.getWsDataString(AbstractWebsocketClient.this.ws));
				}

				this.logInfo(
						"Websocket [" + serverUri.toString() + "] closed. Code [" + code + "] Reason [" + reason + "]");
				AbstractWebsocketClient.this.reconnectorWorker.triggerNextRun();
			}
		};
		// https://github.com/TooTallNate/Java-WebSocket/wiki/Lost-connection-detection
		this.ws.setConnectionLostTimeout(300);

		// initialize WsData
		var wsData = AbstractWebsocketClient.this.createWsData();
		wsData.setWebsocket(this.ws);
		this.ws.setAttachment(wsData);

		// Initialize reconnector
		this.reconnectorWorker = new ClientReconnectorWorker(this);

		if (proxy != null) {
			this.ws.setProxy(proxy);
		}
	}

	/**
	 * Starts the websocket client.
	 */
	@Override
	public void start() {
		this.logInfo(this.log, "Opening connection to websocket server [" + this.serverUri + "]");
		this.reconnectorWorker.activate(this.getName());
		this.reconnectorWorker.triggerNextRun();
	}

	/**
	 * Starts the websocket client; waiting till it started.
	 *
	 * @throws InterruptedException on waiting error
	 */
	public void startBlocking() throws InterruptedException {
		this.logInfo(this.log, "Opening connection to websocket server [" + this.serverUri + "]");
		this.ws.connectBlocking();
		this.reconnectorWorker.activate(this.getName());
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
		this.ws.close(CloseFrame.NORMAL, "Closing connection [" + this.getName() + "]");
	}

	@Override
	protected OnInternalError getOnInternalError() {
		return (t, wsDataString) -> {
			this.logError(this.log,
					"OnInternalError for " + wsDataString + ". " + t.getClass() + ": " + t.getMessage());
			t.printStackTrace();
		};
	}

	/**
	 * Sends a {@link JsonrpcMessage}.
	 *
	 * @param message the {@link JsonrpcMessage}
	 * @throws OpenemsException on error, e.g. if the websocket is not connected
	 */
	public void sendMessageOrError(JsonrpcMessage message) throws OpenemsException {
		try {
			this.ws.send(message.toString());
		} catch (Exception e) {
			if (e instanceof WebsocketNotConnectedException) {
				AbstractWebsocketClient.this.reconnectorWorker.triggerNextRun();
			}
			throw new OpenemsException("Unable to send JSON-RPC message. " + e.getClass().getSimpleName() + ": "
					+ StringUtils.toShortString(message.toString(), 100));
		}
	}

	/**
	 * Sends a JSON-RPC message. Returns true if sending was successful, otherwise
	 * false. Also logs a warning in that case.
	 *
	 * @param message the {@link JsonrpcMessage}.
	 * @return true if sending was successful
	 */
	public boolean sendMessage(JsonrpcMessage message) {
		try {
			this.sendMessageOrError(message);
			return true;

		} catch (OpenemsException e) {
			this.logWarn(this.log, e.getMessage());
			return false;
		}
	}

	/**
	 * Sends a JSON-RPC Request and returns a future Response.
	 *
	 * @param request the JSON-RPC Request
	 * @return the future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> sendRequest(JsonrpcRequest request) throws OpenemsNamedException {
		WsData wsData = this.ws.getAttachment();
		return wsData.send(request);
	}

}
